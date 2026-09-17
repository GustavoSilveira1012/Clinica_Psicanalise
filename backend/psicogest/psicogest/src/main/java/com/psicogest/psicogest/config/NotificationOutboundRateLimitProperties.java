package com.psicogest.psicogest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.notifications.outbound-rate-limit")
public record NotificationOutboundRateLimitProperties(
        Bucket email,
        Bucket whatsapp,
        Bucket sms
) {
    public record Bucket(int capacity, Duration refillPeriod) {
    }
}
