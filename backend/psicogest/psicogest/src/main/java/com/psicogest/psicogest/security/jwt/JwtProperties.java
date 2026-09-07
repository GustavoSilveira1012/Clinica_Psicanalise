package com.psicogest.psicogest.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "app.security.jwt"
)
public record JwtProperties(

        String issuer,

        String audience,

        Duration accessTokenTtl,

        Duration refreshTokenTtl,

        Resource publicKey,

        Resource privateKey,

        String keyId,

        String refreshCookieName,

        boolean secureCookie

) {
}