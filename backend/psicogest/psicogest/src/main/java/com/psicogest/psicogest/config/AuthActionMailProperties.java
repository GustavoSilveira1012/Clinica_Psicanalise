package com.psicogest.psicogest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;
import java.util.Base64;

@ConfigurationProperties(prefix = "app.auth-actions.mail")
public record AuthActionMailProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String from,
        @DefaultValue("") String publicBaseUrl,
        @DefaultValue("") String rateLimitKey,
        @DefaultValue("10") int requestsPerIp,
        @DefaultValue("3") int requestsPerAddress,
        @DefaultValue("PT1H") Duration rateLimitWindow
) {
    public AuthActionMailProperties {
        if (enabled) {
            URI publicUri = URI.create(publicBaseUrl == null ? "" : publicBaseUrl);
            if (!"https".equalsIgnoreCase(publicUri.getScheme())
                    || publicUri.getHost() == null
                    || publicUri.getQuery() != null
                    || publicUri.getFragment() != null) {
                throw new IllegalArgumentException("AUTH_ACTION_PUBLIC_BASE_URL deve ser uma origem HTTPS sem query ou fragmento");
            }
            if (from == null || !from.matches("(?i)^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$")) {
                throw new IllegalArgumentException("AUTH_ACTION_MAIL_FROM deve ser um endereço de e-mail válido");
            }
            try {
                if (Base64.getDecoder().decode(rateLimitKey).length < 32) {
                    throw new IllegalArgumentException("AUTH_ACTION_RATE_LIMIT_KEY deve conter pelo menos 32 bytes");
                }
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("AUTH_ACTION_RATE_LIMIT_KEY deve ser Base64 válido com pelo menos 32 bytes", exception);
            }
            if (requestsPerIp < 1 || requestsPerIp > 100
                    || requestsPerAddress < 1 || requestsPerAddress > 20
                    || rateLimitWindow == null || rateLimitWindow.isZero() || rateLimitWindow.isNegative()) {
                throw new IllegalArgumentException("Limites de solicitação de ação de conta inválidos");
            }
        }
    }
}
