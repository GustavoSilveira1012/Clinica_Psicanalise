package com.psicogest.psicogest.infrastructure.payment.provider.secret;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;

/**
 * Production boundary for credentials materialized by the platform secret
 * manager. A Vault/AWS sidecar may inject the same variables; the application
 * never needs to know the secret-manager protocol and never persists values.
 */
@Component
@Profile("production")
public class VaultPaymentProviderSecretProvider
        implements PaymentProviderSecretProvider {

    @Override
    public String apiCredential(PaymentProviderType provider) {
        return requiredSecret(provider.name() + "_API_KEY");
    }

    @Override
    public String webhookSecret(PaymentProviderType provider) {
        return requiredSecret(provider.name() + "_WEBHOOK_SECRET");
    }

    private String requiredSecret(String environmentVariable) {
        String value = System.getenv(environmentVariable);
        if (value == null || value.isBlank()) {
            throw new PaymentProviderSecretNotFound(
                    "Credencial de provider não fornecida pelo secret manager: "
                            + environmentVariable);
        }
        return value;
    }
}
