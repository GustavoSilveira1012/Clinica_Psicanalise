package com.psicogest.psicogest.infrastructure.payment.provider.webhook;

import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 20. Registry de adapters de webhook
 * 
 * Cada provider implementa sua própria estratégia de validação de webhook
 * (assinatura HMAC, timestamp, etc)
 * 
 * Sem GenericHmacWebhookVerifier como "verdade universal"
 */
@Slf4j
@Component
public class PaymentWebhookAdapterRegistry {

    private final Map<PaymentProviderType, PaymentWebhookAdapter> adapters;

    /**
     * Inicializa com auto-wire de todas as implementações
     */
    public PaymentWebhookAdapterRegistry(
            List<PaymentWebhookAdapter> implementations
    ) {

        this.adapters = Collections.unmodifiableMap(
                implementations.stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        PaymentWebhookAdapter::getType,
                                        Function.identity()
                                )
                        )
        );

        log.info("Webhook adapters registrados: {}", adapters.keySet());
    }

    /**
     * Recupera um adapter por tipo
     * 
     * @throws WebhookAdapterNotConfiguredException se não existe
     */
    public PaymentWebhookAdapter get(
            PaymentProviderType type
    ) {

        PaymentWebhookAdapter adapter =
                adapters.get(type);

        if (adapter == null) {

            throw new WebhookAdapterNotConfiguredException(
                    "Adapter de webhook não configurado: " + type
            );
        }

        return adapter;
    }

    /**
     * Exception customizada
     */
    public static class WebhookAdapterNotConfiguredException
            extends RuntimeException {

        public WebhookAdapterNotConfiguredException(
                String message
        ) {

            super(message);
        }
    }
}
