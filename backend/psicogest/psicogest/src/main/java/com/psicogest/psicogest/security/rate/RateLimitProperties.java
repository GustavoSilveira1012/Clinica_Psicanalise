package com.psicogest.psicogest.security.rate;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "app.security.rate-limit"
)
public record RateLimitProperties(

        Bucket loginIp,
        Bucket loginAccount,

        Bucket refreshIp,

        Bucket mfaIp,
        Bucket mfaChallenge

) {

    public record Bucket(

            int capacity,

            Duration refillPeriod

    ) {
    }
}