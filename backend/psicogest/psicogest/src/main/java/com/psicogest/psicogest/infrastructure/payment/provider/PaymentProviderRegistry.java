package com.psicogest.psicogest.infrastructure.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 6. Registry de providers de pagamento
 * 
 * Em vez de switch(provider) espalhado pelo projeto,
 * centralizamos aqui.
 * 
 * Agora adicionar outro gateway NÃO exige mexer no PaymentService.
 * Apenas implemente PaymentProvider e adicione ao Spring context.
 */
@Slf4j
@Component
public class PaymentProviderRegistry {

    private final Map<PaymentProviderType, PaymentProvider> providers;

    /**
     * Inicializa com auto-wire de todas as implementações
     */
    public PaymentProviderRegistry(
            List<PaymentProvider> implementations
    ) {

        this.providers = Collections.unmodifiableMap(
                implementations.stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        PaymentProvider::type,
                                        Function.identity()
                                )
                        )
        );

        log.info("Providers registrados: {}", providers.keySet());
    }

    /**
     * Recupera um provider por tipo
     * 
     * @throws PaymentProviderNotConfiguredException se não existe
     */
    public PaymentProvider get(
            PaymentProviderType type
    ) {

        PaymentProvider provider = providers.get(type);

        if (provider == null) {

            throw new PaymentProviderNotConfiguredException(
                    "Provedor financeiro não configurado: " + type
            );
        }

        return provider;
    }

    /**
     * Exception customizada
     */
    public static class PaymentProviderNotConfiguredException
            extends RuntimeException {

        public PaymentProviderNotConfiguredException(
                String message
        ) {

            super(message);
        }
    }
}
