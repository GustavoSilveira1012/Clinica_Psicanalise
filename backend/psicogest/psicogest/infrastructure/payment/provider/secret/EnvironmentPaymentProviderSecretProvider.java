package com.psicogest.psicogest.infrastructure.payment.provider.secret;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;

import lombok.extern.slf4j.Slf4j;

/**
 * 42. Implementação DEV: segredos via variáveis de ambiente
 * 
 * Convenção:
 * - API credential: {PROVIDER}_API_KEY
 * - Webhook secret: {PROVIDER}_WEBHOOK_SECRET
 * 
 * Exemplo:
 * STRIPE_API_KEY=sk_test_...
 * STRIPE_WEBHOOK_SECRET=whsec_test_...
 * MERCADO_PAGO_API_KEY=APP_ID_...
 * MERCADO_PAGO_WEBHOOK_SECRET=webhook_secret_...
 */
@Slf4j
@Component
@Profile("dev")
public class EnvironmentPaymentProviderSecretProvider
        implements PaymentProviderSecretProvider {

    @Override
    public String apiCredential(PaymentProviderType provider) {

        String envVar =
                provider.name() + "_API_KEY";

        String value =
                System.getenv(envVar);

        if (value == null || value.isBlank()) {

            throw new PaymentProviderSecretNotFound(
                    "Variável de ambiente não encontrada: " + envVar
            );
        }

        log.debug(
                "API credential carregado do env: provider={}",
                provider
        );

        return value;
    }

    @Override
    public String webhookSecret(PaymentProviderType provider) {

        String envVar =
                provider.name() + "_WEBHOOK_SECRET";

        String value =
                System.getenv(envVar);

        if (value == null || value.isBlank()) {

            throw new PaymentProviderSecretNotFound(
                    "Variável de ambiente não encontrada: " + envVar
            );
        }

        log.debug(
                "Webhook secret carregado do env: provider={}",
                provider
        );

        return value;
    }
}
