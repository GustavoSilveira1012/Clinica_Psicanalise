package com.psicogest.psicogest.infrastructure.saas.billing;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SaasBillingProviderRegistry {

    private final Map<SaasBillingProviderType, SaasBillingProvider> providers;

    public SaasBillingProviderRegistry(List<SaasBillingProvider> implementations) {
        providers = implementations.stream()
                .collect(Collectors.toUnmodifiableMap(
                        SaasBillingProvider::type,
                        Function.identity()
                ));
    }

    public SaasBillingProvider get(SaasBillingProviderType type) {
        SaasBillingProvider provider = providers.get(type);
        if (provider == null) {
            throw new SaasBillingProviderNotConfiguredException(
                    "Provedor de billing SaaS não configurado: " + type);
        }
        return provider;
    }

    public static class SaasBillingProviderNotConfiguredException
            extends RuntimeException {
        public SaasBillingProviderNotConfiguredException(String message) {
            super(message);
        }
    }
}
