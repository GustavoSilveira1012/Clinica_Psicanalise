package com.psicogest.psicogest.infrastructure.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Exposes the external-provider gate through Spring Boot readiness health. */
@Component("externalProviders")
@Profile("production")
public class ProviderReadinessHealthIndicator implements HealthIndicator {

    private final ProviderReadinessService readinessService;

    public ProviderReadinessHealthIndicator(ProviderReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    @Override
    public Health health() {
        Health.Builder builder = readinessService.isReady() ? Health.up() : Health.down();
        return builder.withDetails(readinessService.status()).build();
    }
}
