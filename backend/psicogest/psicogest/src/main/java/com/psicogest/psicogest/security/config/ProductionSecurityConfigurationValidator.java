package com.psicogest.psicogest.security.config;

import com.psicogest.psicogest.security.auth.jwt.JwtProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fails startup instead of silently accepting an unsafe production profile.
 * Local HTTP development keeps its own explicit configuration in the default
 * profile; this guard only applies to deployments using {@code production}.
 */
@Component
@Profile("production")
public class ProductionSecurityConfigurationValidator {

    public ProductionSecurityConfigurationValidator(
            SecurityProperties securityProperties,
            JwtProperties jwtProperties
    ) {
        if (!jwtProperties.secureCookie()) {
            throw new IllegalStateException(
                    "JWT_COOKIE_SECURE=true é obrigatório no perfil production");
        }

        if (securityProperties.allowedOrigins() == null
                || securityProperties.allowedOrigins().isEmpty()
                || securityProperties.allowedOrigins().stream()
                .anyMatch(origin -> origin == null || !origin.startsWith("https://"))) {
            throw new IllegalStateException(
                    "SECURITY_ALLOWED_ORIGINS deve conter apenas origens HTTPS no perfil production");
        }
    }
}
