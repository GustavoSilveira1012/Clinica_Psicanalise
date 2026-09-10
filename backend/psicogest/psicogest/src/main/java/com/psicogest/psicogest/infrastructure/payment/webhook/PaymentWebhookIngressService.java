package com.psicogest.psicogest.infrastructure.payment.webhook;

import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;
import com.psicogest.psicogest.infrastructure.payment.provider.webhook.*;
import com.psicogest.psicogest.model.entity.PaymentWebhookInbox;
import com.psicogest.psicogest.model.entity.SecurityEvent;
import com.psicogest.psicogest.model.enums.SecurityEventType;
import com.psicogest.psicogest.repository.PaymentWebhookInboxRepository;
import com.psicogest.psicogest.repository.SecurityEventRepository;
import com.psicogest.psicogest.security.SecurityHashService;
import com.psicogest.psicogest.security.crypto.ApplicationEncryptionService;
import com.psicogest.psicogest.security.crypto.EncryptedEnvelope;
import com.psicogest.psicogest.security.crypto.EncryptionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 21. Serviço de ingresso de webhooks
 * 
 * Ordem correta:
 * 1. Limite de tamanho
 * 2. Adapter verifica assinatura
 * 3. Verifica timestamp/replay
 * 4. Parse normalizado
 * 5. Verifica eventId
 * 6. Persist inbox
 * 7. Retorna 200
 * 
 * Nunca persistir/processar evento não verificado
 */
@Slf4j
@Service
public class PaymentWebhookIngressService {

    private static final int MAX_WEBHOOK_SIZE =
            512 * 1024; // 512 KB

    private final PaymentWebhookAdapterRegistry adapterRegistry;
    private final PaymentWebhookInboxRepository inboxRepository;
    private final ApplicationEncryptionService encryptionService;
    private final SecurityHashService hashService;
    private final SecurityEventRepository securityEventRepository;
    private final Clock clock;

    public PaymentWebhookIngressService(
            PaymentWebhookAdapterRegistry adapterRegistry,
            PaymentWebhookInboxRepository inboxRepository,
            ApplicationEncryptionService encryptionService,
            SecurityHashService hashService,
            SecurityEventRepository securityEventRepository,
            Clock clock
    ) {
        this.adapterRegistry = adapterRegistry;
        this.inboxRepository = inboxRepository;
        this.encryptionService = encryptionService;
        this.hashService = hashService;
        this.securityEventRepository = securityEventRepository;
        this.clock = clock;
    }

    /**
     * 22. Processa webhook recebido
     * 
     * @param provider tipo de provider
     * @param rawBody bytes brutos do webhook
     * @param headers headers HTTP
     * @param sourceIp IP de origem
     * @throws PaymentWebhookAdapter.InvalidSignatureException se assinatura inválida
     */
    @Transactional
    public void receive(
            PaymentProviderType provider,
            byte[] rawBody,
            Map<String, String> headers,
            String sourceIp
    ) throws PaymentWebhookAdapter.InvalidSignatureException {

        Instant receivedAt = clock.instant();

        try {

            // 22. Limite de tamanho
            if (rawBody.length > MAX_WEBHOOK_SIZE) {

                logSecurityEvent(
                        provider,
                        SecurityEventType.WEBHOOK_SIGNATURE_INVALID,
                        "Webhook inválido",
                        sourceIp,
                        Map.of("reason", "tamanho_excedido")
                );

                throw new PaymentWebhookAdapter.InvalidSignatureException(
                        "Webhook inválido"
                );
            }

            // 23. Adapter verifica assinatura
            WebhookRequest webhookRequest =
                    new WebhookRequest(
                            rawBody,
                            headers,
                            receivedAt,
                            sourceIp
                    );

            PaymentWebhookAdapter adapter =
                    adapterRegistry.get(provider);

            VerifiedWebhook verified;

            try {

                verified = adapter.verifyAndParse(
                        webhookRequest
                );

            } catch (
                    PaymentWebhookAdapter.InvalidSignatureException e
            ) {

                logSecurityEvent(
                        provider,
                        SecurityEventType.WEBHOOK_SIGNATURE_INVALID,
                        e.getMessage(),
                        sourceIp,
                        Map.of(
                                "eventId",
                                headers.getOrDefault(
                                        "X-Event-ID",
                                        "unknown"
                                )
                        )
                );

                throw e;
            }

            // 26. Hash do payload
            String payloadHash =
                    hashService.sha256(rawBody);

            if (!payloadHash.equals(
                    verified.payloadSha256()
            )) {

                log.error(
                        "Inconsistência interna de webhook: " +
                                "hash inválido"
                );

                throw new IllegalStateException(
                        "Inconsistência interna do webhook"
                );
            }

            // 27. Idempotência: buscar evento existente
            Optional<PaymentWebhookInbox> existing =
                    inboxRepository.findByProviderAndProviderEventId(
                            provider,
                            verified.providerEventId()
                    );

            if (existing.isPresent()) {

                PaymentWebhookInbox inbox =
                        existing.get();

                // Mesmo evento, mesmo hash = duplicata legítima
                if (payloadHash.equals(
                        inbox.getPayloadSha256()
                )) {

                    log.info(
                            "Webhook duplicado recebido (legítimo): " +
                                    "provider={}, eventId={}",
                            provider,
                            verified.providerEventId()
                    );

                    logSecurityEvent(
                            provider,
                            SecurityEventType
                                    .WEBHOOK_DUPLICATE_RECEIVED,
                            "Webhook duplicado recebido",
                            sourceIp,
                            Map.of(
                                    "eventId",
                                    verified.providerEventId()
                            )
                    );

                    // Retorna 200
                    return;
                }

                // Mesmo evento, hash diferente = suspeito
                log.warn(
                        "Colisão de evento de webhook: " +
                                "provider={}, eventId={}, " +
                                "hash antigo={}, hash novo={}",
                        provider,
                        verified.providerEventId(),
                        inbox.getPayloadSha256(),
                        payloadHash
                );

                logSecurityEvent(
                        provider,
                        SecurityEventType.WEBHOOK_EVENT_COLLISION,
                        "Colisão de evento",
                        sourceIp,
                        Map.of(
                                "eventId",
                                verified.providerEventId(),
                                "hashAntigo",
                                inbox.getPayloadSha256(),
                                "hashNovo",
                                payloadHash
                        )
                );

                // Não processamos o segundo
                return;
            }

            // 29. Persistindo inbox
            UUID inboxId = UUID.randomUUID();

            EncryptionContext cryptoContext =
                    new EncryptionContext(
                            "PAYMENT_WEBHOOK",
                            inboxId.toString(),
                            "payload",
                            Map.of(
                                    "provider",
                                    provider.name(),
                                    "eventId",
                                    verified.providerEventId()
                            )
                    );

            String payloadJson =
                    new String(
                            rawBody,
                            StandardCharsets.UTF_8
                    );

            EncryptedEnvelope encrypted =
                    encryptionService.encrypt(
                            payloadJson,
                            cryptoContext
                    );

            PaymentWebhookInbox inbox =
                    PaymentWebhookInbox.builder()
                            .id(inboxId)
                            .provider(provider)
                            .providerEventId(
                                    verified.providerEventId()
                            )
                            .eventType(
                                    verified.event()
                                            .type()
                                            .name()
                            )
                            .payloadSha256(payloadHash)
                            .status(
                                    WebhookInboxStatus.RECEIVED
                            )
                            .attemptCount(0)
                            .encryptedPayload(
                                    encrypted.ciphertext()
                            )
                            .payloadIv(encrypted.iv())
                            .encryptedDek(
                                    encrypted.wrappedDataKey()
                            )
                            .cryptoVersion(
                                    encrypted.cryptoVersion()
                            )
                            .cryptoAlgorithm(
                                    encrypted.algorithm()
                            )
                            .keyId(encrypted.keyId())
                            .receivedAt(receivedAt)
                            .createdAt(receivedAt)
                            .build();

            inboxRepository.saveAndFlush(inbox);

            log.info(
                    "Webhook armazenado no inbox: " +
                            "id={}, provider={}, eventId={}",
                    inboxId,
                    provider,
                    verified.providerEventId()
            );

        } catch (Exception e) {

            log.error(
                    "Falha ao ingressar webhook: provider={}, error={}",
                    provider,
                    e.getMessage(),
                    e
            );

            logSecurityEvent(
                    provider,
                    SecurityEventType.WEBHOOK_PROCESSING_FAILED,
                    e.getMessage(),
                    sourceIp,
                    Map.of("reason", e.getClass()
                            .getSimpleName())
            );

            throw e;
        }
    }

    /**
     * Loga evento de segurança
     * 
     * Nunca loga o payload completo
     */
    private void logSecurityEvent(
            PaymentProviderType provider,
            SecurityEventType eventType,
            String description,
            String sourceIp,
            Map<String, String> metadata
    ) {

        Map<String, Object> metadataObj =
                new HashMap<>(metadata);

        metadataObj.put("description", description);

        SecurityEvent event = SecurityEvent
                .builder()
                .id(UUID.randomUUID())
                .eventType(eventType)
                .sourceIp(sourceIp)
                .metadata(metadataObj)
                .build();

        securityEventRepository.save(event);
    }
}
