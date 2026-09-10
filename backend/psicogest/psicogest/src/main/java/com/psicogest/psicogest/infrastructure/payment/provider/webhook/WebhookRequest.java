package com.psicogest.psicogest.infrastructure.payment.provider.webhook;

import java.time.Instant;
import java.util.Map;

/**
 * 8. Webhook bruto recebido do provider
 * 
 * Importantíssimo: precisamos dos BYTES EXATAMENTE como chegaram.
 * 
 * Não faça:
 *   JSON → ObjectMapper → objeto → serializar → verificar assinatura ❌
 * 
 * Espaços, ordenação ou serialização podem mudar os bytes
 * e invalidar a assinatura.
 * 
 * Serviços que assinam webhooks explicitamente exigem o corpo bruto.
 */
public record WebhookRequest(

        /**
         * Bytes brutos do body (clonados para imutabilidade)
         */
        byte[] rawBody,

        /**
         * Headers HTTP
         * Pode incluir: X-Signature, X-Timestamp, etc
         */
        Map<String, String> headers,

        /**
         * Quando foi recebido
         */
        Instant receivedAt,

        /**
         * IP de origem
         */
        String sourceIp

) {

    /**
     * Compact constructor para clonar rawBody
     * (defesa contra mutações)
     */
    public WebhookRequest {

        rawBody = rawBody.clone();
    }

    /**
     * Override para retornar clone
     * (nunca expor array mutável)
     */
    @Override
    public byte[] rawBody() {

        return rawBody.clone();
    }
}
