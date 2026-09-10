package com.psicogest.psicogest.infrastructure.payment.provider.webhook;

import java.time.Instant;
import java.util.Map;

/**
 * 9. Webhook verificado e desserializado
 * 
 * Assinatura já foi validada.
 * providerEventId é fundamental para idempotência.
 * 
 * Se o gateway mandar:
 *   evt_ABC123
 *   evt_ABC123
 *   evt_ABC123
 * 
 * Processaremos EXATAMENTE UMA VEZ.
 */
public record VerifiedWebhook(

        /**
         * ID único do evento no provider
         * Ex: evt_ABC123, ch_123456789, etc
         * Essencial para deduplicação
         */
        String providerEventId,

        /**
         * Quando foi criado no provider
         */
        Instant providerCreatedAt,

        /**
         * Evento normalizado
         */
        PaymentProviderEvent event,

        /**
         * SHA256 do payload (para auditoria)
         */
        String payloadSha256

) {
}
