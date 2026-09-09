package com.psicogest.psicogest.security;

import com.psicogest.psicogest.security.authorization.SecurityContextService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Factory para construir SecurityActor a partir de Authentication e HttpServletRequest
 * Centraliza a lógica de extração de dados de segurança
 */
@Slf4j
@Component
public class SecurityActorFactory {

    private final SecurityContextService contextService;

    public SecurityActorFactory(SecurityContextService contextService) {
        this.contextService = contextService;
    }

    /**
     * Constrói SecurityActor a partir de Authentication e HttpServletRequest
     */
    public SecurityActor from(
            Authentication authentication,
            HttpServletRequest request
    ) {
        Long userId = contextService
                .userId(authentication)
                .orElseThrow(() -> new IllegalArgumentException("userId não encontrado no authentication"));

        // Extrair dados do request
        String sourceIp = extractClientIp(request);
        String correlationId = extractCorrelationId(request);
        String userAgentHash = hashUserAgent(request.getHeader("User-Agent"));
        UUID sessionId = extractSessionId(request);

        log.debug("SecurityActor construído: userId={}, correlationId={}, sourceIp={}",
                userId, correlationId, sourceIp);

        return new SecurityActor(
                userId,
                sessionId,
                correlationId,
                sourceIp,
                userAgentHash
        );
    }

    /**
     * Extrai IP do cliente (considerando proxies)
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Extrai correlationId do header ou gera um novo
     */
    private String extractCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Extrai sessionId do header ou gera um novo
     */
    private UUID extractSessionId(HttpServletRequest request) {
        String sessionIdStr = request.getHeader("X-Session-ID");
        if (sessionIdStr != null && !sessionIdStr.isBlank()) {
            try {
                return UUID.fromString(sessionIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("X-Session-ID inválido: {}", sessionIdStr);
            }
        }
        return UUID.randomUUID();
    }

    /**
     * Hash do User-Agent para fingerprinting
     */
    private String hashUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }
        return Integer.toHexString(userAgent.hashCode());
    }
}
