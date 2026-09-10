package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;
import com.psicogest.psicogest.infrastructure.payment.provider.webhook.WebhookInboxStatus;
import com.psicogest.psicogest.model.entity.PaymentWebhookInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 19. Repository para PaymentWebhookInbox
 * 
 * Suporta idempotência, retry e deduplicação
 */
@Repository
public interface PaymentWebhookInboxRepository
        extends JpaRepository<PaymentWebhookInbox, UUID> {

    /**
     * Busca webhook por provider e eventId
     * 
     * Essencial para idempotência:
     * Se mesmo (provider, providerEventId) chegou 3x,
     * retorna a primeira instância
     */
    Optional<PaymentWebhookInbox> findByProviderAndProviderEventId(
            PaymentProviderType provider,
            String providerEventId
    );

    /**
     * Lista webhooks pendentes de processamento
     * 
     * Para workers/schedulers retentarem
     */
    List<PaymentWebhookInbox> findByStatusAndNextRetryAtBefore(
            WebhookInboxStatus status,
            Instant now
    );

    /**
     * Lista webhooks que falharam e precisam de atenção
     */
    List<PaymentWebhookInbox> findByStatusAndAttemptCountGreaterThan(
            WebhookInboxStatus status,
            Integer attemptThreshold
    );

    /**
     * Verifica colisão de eventos
     * 
     * Se chegou (provider, eventId) com payloadHash diferente,
     * é comportamento suspeito
     */
    Optional<PaymentWebhookInbox> findByProviderAndProviderEventIdAndPayloadSha256Not(
            PaymentProviderType provider,
            String providerEventId,
            String payloadSha256
    );

    /**
     * Conta webhooks no status RECEIVED mas nunca processados
     * 
     * Para monitoramento
     */
    long countByStatusAndAttemptCountEquals(
            WebhookInboxStatus status,
            Integer attemptCount
    );
}
