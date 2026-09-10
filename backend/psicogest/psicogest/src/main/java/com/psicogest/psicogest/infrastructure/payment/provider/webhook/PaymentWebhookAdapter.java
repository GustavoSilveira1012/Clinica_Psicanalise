package com.psicogest.psicogest.infrastructure.payment.provider.webhook;

import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;

/**
 * 7. Interface para adapters de webhook
 * 
 * Cada gateway assina webhooks de forma diferente.
 * Portanto não fazemos GenericHmacWebhookVerifier como verdade universal.
 * 
 * Cada provider implementa sua própria estratégia de verificação.
 */
public interface PaymentWebhookAdapter {

    /**
     * Tipo de provider que esse adapter suporta
     */
    PaymentProviderType getType();

    /**
     * Valida assinatura e desserializa o webhook
     * 
     * @param request webhook bruto
     * @return webhook verificado
     * @throws InvalidSignatureException se assinatura for inválida
     */
    VerifiedWebhook verifyAndParse(
            WebhookRequest request
    ) throws InvalidSignatureException;

    /**
     * Exceção para assinatura inválida
     */
    class InvalidSignatureException extends Exception {

        public InvalidSignatureException(String message) {

            super(message);
        }

        public InvalidSignatureException(
                String message,
                Throwable cause
        ) {

            super(message, cause);
        }
    }
}
