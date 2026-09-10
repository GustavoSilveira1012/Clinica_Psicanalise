package com.psicogest.psicogest.infrastructure.payment.provider.secret;

import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;

/**
 * 42. Interface para obter segredos de provedores de pagamento
 * 
 * Nunca hardcode API keys. Sempre use abstração.
 * 
 * DEV: EnvironmentPaymentProviderSecretProvider (env vars)
 * PROD: VaultPaymentProviderSecretProvider (HashiCorp Vault / AWS Secrets Manager)
 */
public interface PaymentProviderSecretProvider {

    /**
     * Retorna credencial de API (API key, token, etc)
     * 
     * @param provider tipo de provider
     * @return credencial
     * @throws PaymentProviderSecretNotFound se segredo não existir
     */
    String apiCredential(PaymentProviderType provider);

    /**
     * Retorna segredo de webhook (HMAC secret, assinatura, etc)
     * 
     * @param provider tipo de provider
     * @return segredo de webhook
     * @throws PaymentProviderSecretNotFound se segredo não existir
     */
    String webhookSecret(PaymentProviderType provider);

    /**
     * Exception para segredo não encontrado
     */
    class PaymentProviderSecretNotFound extends RuntimeException {

        public PaymentProviderSecretNotFound(String message) {
            super(message);
        }

        public PaymentProviderSecretNotFound(
                String message,
                Throwable cause
        ) {
            super(message, cause);
        }
    }
}
