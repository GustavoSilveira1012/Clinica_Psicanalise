package com.psicogest.psicogest.service.payment.webhook;

import java.time.Clock;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;
import com.psicogest.psicogest.infrastructure.payment.provider.webhook.PaymentProviderEvent;
import com.psicogest.psicogest.infrastructure.payment.provider.webhook.PaymentProviderEventType;
import com.psicogest.psicogest.infrastructure.payment.provider.webhook.PaymentWebhookAdapterRegistry;
import com.psicogest.psicogest.infrastructure.payment.provider.webhook.WebhookInboxStatus;
import com.psicogest.psicogest.model.entity.PaymentWebhookInbox;
import com.psicogest.psicogest.model.entity.SecurityEvent;
import com.psicogest.psicogest.model.enums.SecurityEventType;
import com.psicogest.psicogest.repository.PaymentWebhookInboxRepository;
import com.psicogest.psicogest.repository.SecurityEventRepository;
import com.psicogest.psicogest.security.crypto.ApplicationEncryptionService;
import com.psicogest.psicogest.security.crypto.EncryptedEnvelope;
import com.psicogest.psicogest.security.crypto.EncryptionContext;
import com.psicogest.psicogest.service.PaymentService;
import com.psicogest.psicogest.service.RefundService;

import lombok.extern.slf4j.Slf4j;

/**
 * 32-43. Processador de webhooks de pagamento
 * 
 * Orquestração:
 * 1. Claim atômico com lock pessimista
 * 2. Parse do payload criptografado
 * 3. Switch de tipos de evento
 * 4. Chamadas aos serviços (sem lock de DB durante operação)
 * 5. Atualização de status no inbox
 * 6. Retry com backoff exponencial
 */
@Slf4j
@Service
@Transactional
public class PaymentWebhookProcessor {

    private final PaymentWebhookInboxRepository inboxRepository;

    private final PaymentWebhookAdapterRegistry adapterRegistry;

    private final PaymentService paymentService;

    private final RefundService refundService;

    private final ApplicationEncryptionService encryptionService;

    private final SecurityEventRepository securityEventRepository;

    private final Clock clock;
    private final ObjectMapper objectMapper;

    public PaymentWebhookProcessor(
            PaymentWebhookInboxRepository inboxRepository,
            PaymentWebhookAdapterRegistry adapterRegistry,
            PaymentService paymentService,
            RefundService refundService,
            ApplicationEncryptionService encryptionService,
            SecurityEventRepository securityEventRepository,
            Clock clock,
            ObjectMapper objectMapper
    ) {
        this.inboxRepository = inboxRepository;
        this.adapterRegistry = adapterRegistry;
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.encryptionService = encryptionService;
        this.securityEventRepository = securityEventRepository;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    /**
     * 32-43. Processa webhook do inbox
     * 
     * Padrão transactional inbox com atomic claim:
     * 1. Busca inbox com PESSIMISTIC_WRITE lock
     * 2. Verifica status (evita reprocessamento)
     * 3. Marca como PROCESSING e salva (libera lock brevemente)
     * 4. Descriptografa payload
     * 5. Parse para PaymentProviderEvent
     * 6. Switch de tipos → chamadas aos serviços
     * 7. Atualiza status para PROCESSED
     * 
     * @param inboxId ID do inbox
     * @throws ResourceNotFoundException se inbox não encontrado
     */
    @Transactional
    public void process(UUID inboxId) {

        Instant now = clock.instant();

        // 32. Claim atômico: lock pessimista
        PaymentWebhookInbox inbox =
                inboxRepository
                        .findByIdForUpdate(inboxId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Webhook inbox não encontrado"
                                        )
                        );

        // 32. Verificar se já foi processado
        if (
                inbox.getStatus()
                        == WebhookInboxStatus.PROCESSED
        ) {

            log.info(
                    "Webhook já processado: id={}, provider={}, eventId={}",
                    inboxId,
                    inbox.getProvider(),
                    inbox.getProviderEventId()
            );

            return;
        }

        // 32. Verificar se já está sendo processado
        if (
                inbox.getStatus()
                        == WebhookInboxStatus.PROCESSING
        ) {

            log.warn(
                    "Webhook já em processamento: id={}, provider={}, eventId={}",
                    inboxId,
                    inbox.getProvider(),
                    inbox.getProviderEventId()
            );

            return;
        }

        // 32. Marca como processando
        inbox.setStatus(
                WebhookInboxStatus.PROCESSING
        );

        inbox.setProcessingStartedAt(now);

        inboxRepository.saveAndFlush(inbox);

        try {

            // Descriptografar payload
            String payloadJson =
                    decryptPayload(inbox);

            // Parse normalizado
            PaymentProviderEvent event =
                    parseEvent(
                            inbox.getProvider(),
                            inbox.getEventType(),
                            payloadJson
                    );

            // 34-38. Switch de tipos de evento
            switch (event.type()) {

                case PAYMENT_CONFIRMED ->

                        paymentService
                                .confirmFromProvider(

                                        inbox.getProvider(),

                                        event.providerTransactionId(),

                                        event.occurredAt()
                                );

                case PAYMENT_FAILED ->

                        paymentService
                                .failFromProvider(

                                        inbox.getProvider(),

                                        event.providerTransactionId(),

                                        event.occurredAt()
                                );

                case REFUND_CONFIRMED ->

                        refundService
                                .confirmFromProvider(

                                        event.providerRefundId(),

                                        inbox.getProvider(),

                                        event.occurredAt()
                                );

                case REFUND_FAILED ->

                        refundService
                                .failFromProvider(

                                        event.providerRefundId(),

                                        inbox.getProvider(),

                                        event.occurredAt()
                                );

                case PAYMENT_CANCELLED ->

                        log.info(
                                "Evento PAYMENT_CANCELLED recebido: " +
                                        "provider={}, eventId={}",
                                inbox.getProvider(),
                                inbox.getProviderEventId()
                        );

                default ->

                        log.warn(
                                "Tipo de evento desconhecido: " +
                                        "provider={}, eventId={}, type={}",
                                inbox.getProvider(),
                                inbox.getProviderEventId(),
                                event.type()
                        );
            }

            // Marcar como processado
            inbox.setStatus(
                    WebhookInboxStatus.PROCESSED
            );

            inbox.setProcessedAt(now);

            inbox.setAttemptCount(
                    inbox.getAttemptCount() + 1
            );

            inboxRepository.saveAndFlush(inbox);

            log.info(
                    "Webhook processado com sucesso: " +
                            "id={}, provider={}, eventId={}, type={}",
                    inboxId,
                    inbox.getProvider(),
                    inbox.getProviderEventId(),
                    inbox.getEventType()
            );

        } catch (Exception e) {

            handleProcessingError(
                    inbox,
                    e,
                    now
            );
        }
    }

    /**
     * Descriptografa payload do webhook
     */
    private String decryptPayload(PaymentWebhookInbox inbox) {
        EncryptionContext context = new EncryptionContext(
                "PAYMENT_WEBHOOK", inbox.getId().toString(), "payload",
                Map.of("provider", inbox.getProvider().name(), "eventId", inbox.getProviderEventId()));
        return encryptionService.decrypt(new EncryptedEnvelope(
                inbox.getCryptoVersion(), inbox.getCryptoAlgorithm(), inbox.getKeyId(),
                inbox.getEncryptedDek(), inbox.getPayloadIv(), inbox.getEncryptedPayload()), context);
    }

    /**
     * Faz parse do payload para PaymentProviderEvent
     */
    private PaymentProviderEvent parseEvent(
            PaymentProviderType provider,
            String eventType,
            String payloadJson
    ) {
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            PaymentProviderEventType type = PaymentProviderEventType.valueOf(eventType);
            String transactionId = text(root, "providerTransactionId", "transaction_id", "transactionId", "payment_id");
            String refundId = text(root, "providerRefundId", "refund_id", "refundId");
            if (transactionId == null) transactionId = nestedText(root, "data", "object", "id");
            if (refundId == null && (type == PaymentProviderEventType.REFUND_CONFIRMED
                    || type == PaymentProviderEventType.REFUND_FAILED)) refundId = nestedText(root, "data", "object", "refund_id");
            if ((type == PaymentProviderEventType.REFUND_CONFIRMED || type == PaymentProviderEventType.REFUND_FAILED)
                    && (refundId == null || refundId.isBlank())) {
                throw new IllegalArgumentException("Webhook sem identificador de reembolso");
            }
            if (type != PaymentProviderEventType.REFUND_CONFIRMED && type != PaymentProviderEventType.REFUND_FAILED
                    && (transactionId == null || transactionId.isBlank())) {
                throw new IllegalArgumentException("Webhook sem identificador de transação");
            }
            BigDecimal amount = decimal(root, "amount", "value", "transaction_amount");
            String occurred = text(root, "occurredAt", "occurred_at", "created_at");
            Instant occurredAt = occurred == null ? clock.instant() : Instant.parse(occurred);
            return new PaymentProviderEvent(type, transactionId, refundId, amount, occurredAt);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Payload de webhook inválido para " + provider, exception);
        }
    }

    private String text(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode value = root.get(name);
            if (value != null && value.isValueNode() && !value.asText().isBlank()) return value.asText();
        }
        return null;
    }

    private String nestedText(JsonNode root, String... path) {
        JsonNode current = root;
        for (String part : path) current = current == null ? null : current.get(part);
        return current == null || current.isMissingNode() || current.isNull() ? null : current.asText();
    }

    private BigDecimal decimal(JsonNode root, String... names) {
        String value = text(root, names);
        return value == null ? null : new BigDecimal(value);
    }

    /**
     * Trata erro durante processamento
     * 
     * Classifica como retryable ou não
     * Calcula backoff
     * Atualiza inbox
     */
    private void handleProcessingError(
            PaymentWebhookInbox inbox,
            Exception e,
            Instant now
    ) {

        boolean retryable =
                isRetryable(e);

        WebhookInboxStatus newStatus =
                retryable
                        ? WebhookInboxStatus.FAILED
                        : WebhookInboxStatus.DEAD_LETTER;

        // Calcular próxima tentativa
        Instant nextRetry =
                calculateNextRetry(
                        inbox.getAttemptCount()
                );

        inbox.setStatus(newStatus);

        inbox.setFailedAt(now);

        inbox.setAttemptCount(
                inbox.getAttemptCount() + 1
        );

        inbox.setLastErrorCode(
                e.getClass().getSimpleName()
        );

        inbox.setNextRetryAt(nextRetry);

        inboxRepository.saveAndFlush(inbox);

        // Log de segurança
        logSecurityEvent(
                inbox,
                e
        );

        log.error(
                "Erro ao processar webhook: " +
                        "id={}, provider={}, eventId={}, retryable={}, nextRetry={}",
                inbox.getId(),
                inbox.getProvider(),
                inbox.getProviderEventId(),
                retryable,
                nextRetry,
                e
        );
    }

    /**
     * Classifica se erro é retryable
     */
    private boolean isRetryable(Exception e) {

        // Erros permanentes: não retry
        String message = e.getMessage();

        if (message != null) {

            if (
                    message.contains("assinatura")
                    ||
                    message.contains("inválido")
                    ||
                    message.contains("provider não encontrado")
            ) {

                return false;
            }
        }

        // Erros temporários: retry
        return true;
    }

    /**
     * 38. Calcula próxima tentativa com backoff exponencial
     * 
     * Tentativa 1: 30s
     * Tentativa 2: 2min
     * Tentativa 3: 10min
     * ...
     * Tentativa 8+: DEAD_LETTER
     */
    private Instant calculateNextRetry(
            Integer attemptCount
    ) {

        if (attemptCount >= 8) {

            // Máximo de tentativas atingido
            return null;
        }

        // Backoff: 30s * 2^(attempt-1)
        long backoffSeconds =
                30L * ((long) Math.pow(2, attemptCount));

        return clock.instant()
                .plusSeconds(backoffSeconds);
    }

    /**
     * Log de evento de segurança
     */
    private void logSecurityEvent(
            PaymentWebhookInbox inbox,
            Exception e
    ) {

        SecurityEvent event =
                SecurityEvent.builder()

                        .id(UUID.randomUUID())

                        .eventType(
                                SecurityEventType
                                        .WEBHOOK_PROCESSING_FAILED
                        )

                        .metadata(
                                Map.of(
                                        "provider",
                                        inbox.getProvider()
                                                .name(),

                                        "eventId",
                                        inbox
                                                .getProviderEventId(),

                                        "error",
                                        java.util.Objects.toString(e.getMessage(), e.getClass().getSimpleName())
                                )
                        )

                        .build();

        securityEventRepository.save(event);
    }
}
