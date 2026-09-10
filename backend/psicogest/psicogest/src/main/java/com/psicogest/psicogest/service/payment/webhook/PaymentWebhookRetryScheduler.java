package com.psicogest.psicogest.service.payment.webhook;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.infrastructure.payment.provider.webhook.WebhookInboxStatus;
import com.psicogest.psicogest.model.entity.PaymentWebhookInbox;
import com.psicogest.psicogest.repository.PaymentWebhookInboxRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 38-43. Scheduler de retry para webhooks falhados
 * 
 * Padrão:
 * 1. Query webhooks com status FAILED e nextRetryAt <= now
 * 2. Calcular tentativa (attempt_count)
 * 3. Se attempt >= max_attempts: mover para DEAD_LETTER
 * 4. Se attempt < max_attempts: reprocessar via PaymentWebhookProcessor
 * 5. Backoff exponencial: 30s, 2min, 10min, ...
 * 
 * Frequência: a cada 30 segundos (configurable)
 */
@Slf4j
@Service
public class PaymentWebhookRetryScheduler {

    private final PaymentWebhookInboxRepository inboxRepository;

    private final PaymentWebhookProcessor processor;

    private final Clock clock;

    @Value("${app.payment.webhook.max-attempts:8}")
    private Integer maxAttempts;

    @Value("${app.payment.webhook.retry-batch-size:10}")
    private Integer retryBatchSize;

    public PaymentWebhookRetryScheduler(
            PaymentWebhookInboxRepository inboxRepository,
            PaymentWebhookProcessor processor,
            Clock clock
    ) {
        this.inboxRepository = inboxRepository;
        this.processor = processor;
        this.clock = clock;
    }

    /**
     * 38-43. Executa retry de webhooks falhados
     * 
     * Agendado a cada 30 segundos
     */
    @Scheduled(fixedRateString = "${app.payment.webhook.retry-interval-ms:30000}")
    @Transactional
    public void retryFailed() {

        Instant now = clock.instant();

        // Query webhooks prontos para retry
        List<PaymentWebhookInbox> failedWebhooks =
                inboxRepository
                        .findByStatusAndNextRetryAtBefore(
                                WebhookInboxStatus.FAILED,
                                now
                        );

        if (failedWebhooks.isEmpty()) {

            log.debug(
                    "Nenhum webhook pronto para retry"
            );

            return;
        }

        // Limitar batch size
        List<PaymentWebhookInbox> batch =
                failedWebhooks.stream()
                        .limit(retryBatchSize)
                        .toList();

        log.info(
                "Iniciando retry de {} webhooks falhados",
                batch.size()
        );

        for (PaymentWebhookInbox webhook : batch) {

            try {

                retryWebhook(webhook, now);

            } catch (Exception e) {

                log.error(
                        "Erro ao processar webhook em retry: " +
                                "id={}, provider={}, eventId={}",
                        webhook.getId(),
                        webhook.getProvider(),
                        webhook.getProviderEventId(),
                        e
                );
            }
        }

        log.info(
                "Retry batch completo: {} webhooks processados",
                batch.size()
        );
    }

    /**
     * Reprocessa um webhook falhado
     * 
     * Se atingiu max_attempts: mover para DEAD_LETTER
     * Senão: chamar processor.process()
     */
    private void retryWebhook(
            PaymentWebhookInbox webhook,
            Instant now
    ) {

        Integer attemptCount =
                webhook.getAttemptCount() != null
                        ? webhook.getAttemptCount()
                        : 0;

        // Verificar se atingiu limite
        if (attemptCount >= maxAttempts) {

            log.warn(
                    "Webhook atingiu limite de tentativas: " +
                            "id={}, provider={}, eventId={}, attempts={}",
                    webhook.getId(),
                    webhook.getProvider(),
                    webhook.getProviderEventId(),
                    attemptCount
            );

            webhook.setStatus(
                    WebhookInboxStatus.DEAD_LETTER
            );

            webhook.setFailedAt(now);

            inboxRepository.saveAndFlush(webhook);

            return;
        }

        log.info(
                "Retry do webhook: " +
                        "id={}, provider={}, eventId={}, attempt={}/{}",
                webhook.getId(),
                webhook.getProvider(),
                webhook.getProviderEventId(),
                attemptCount + 1,
                maxAttempts
        );

        // Reprocessar
        processor.process(webhook.getId());
    }
}
