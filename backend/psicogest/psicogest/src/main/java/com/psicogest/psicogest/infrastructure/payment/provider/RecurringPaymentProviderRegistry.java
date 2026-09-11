package com.psicogest.psicogest.infrastructure.payment.provider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class RecurringPaymentProviderRegistry {

    private final Map<PaymentProviderType, RecurringPaymentProvider> providers;

    public RecurringPaymentProviderRegistry(List<RecurringPaymentProvider> providers) {
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                RecurringPaymentProvider::type, Function.identity()));
    }

    public RecurringPaymentProvider get(PaymentProviderType type) {
        RecurringPaymentProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalStateException("Provedor recorrente não configurado: " + type);
        }
        return provider;
    }
}
