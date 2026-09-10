package com.psicogest.psicogest.infrastructure.payment.provider;

import com.psicogest.psicogest.infrastructure.payment.provider.webhook.VerifiedWebhook;
import com.psicogest.psicogest.infrastructure.payment.provider.webhook.WebhookRequest;

/**
 * 2. Interface para integração com gateways de pagamento
 * 
 * O domínio financeiro SÓ conversa com isso.
 * 
 * Nunca teremos em PaymentService:
 *   new MercadoPagoClient(...)
 *   new StripeClient(...)
 * 
 * Cada gateway implementa essa interface.
 * Registry injeta a implementação correta.
 */
public interface PaymentProvider {

    /**
     * Retorna o tipo de provider que essa classe implementa
     */
    PaymentProviderType type();

    /**
     * Cria um pagamento no provider
     * 
     * Deve ser idempotente via idempotencyKey
     */
    ProviderPaymentResult createPayment(
            ProviderPaymentRequest request
    );

    /**
     * Solicita um reembolso no provider
     * 
     * Deve ser idempotente via idempotencyKey
     */
    ProviderRefundResult requestRefund(
            ProviderRefundRequest request
    );

    /**
     * Valida e desserializa um webhook do provider
     * 
     * Verifica assinatura, evita replay attacks
     */
    VerifiedWebhook verifyAndParseWebhook(
            WebhookRequest request
    ) throws InvalidWebhookException;

    /**
     * Exceção para webhooks inválidos
     */
    class InvalidWebhookException extends Exception {

        public InvalidWebhookException(String message) {

            super(message);
        }

        public InvalidWebhookException(
                String message,
                Throwable cause
        ) {

            super(message, cause);
        }
    }
}
