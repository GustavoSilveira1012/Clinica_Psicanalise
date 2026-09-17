package com.psicogest.psicogest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.saas")
public record SaasCommercialProperties(int trialDays) {
}
