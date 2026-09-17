package com.psicogest.psicogest.security;

import com.psicogest.psicogest.security.authorization.SecurityContextService;
import com.psicogest.psicogest.infrastructure.security.SecurityHashService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
    private final SecurityHashService hashService;

    public SecurityActorFactory(SecurityContextService contextService, SecurityHashService hashService) {
        this.contextService = contextService;
        this.hashService = hashService;
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
        UUID sessionId = extractSessionId(authentication);

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
    private UUID extractSessionId(Authentication authentication) {
        String sessionIdStr = authentication instanceof JwtAuthenticationToken jwt
                ? jwt.getToken().getClaimAsString("sid") : null;
        if (sessionIdStr != null && !sessionIdStr.isBlank()) {
            try {
                return UUID.fromString(sessionIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("X-Session-ID inválido: {}", sessionIdStr);
            }
        }
        return null;
    }

    /**
     * Hash do User-Agent para fingerprinting
     */
    private String hashUserAgent(String userAgent) {
        return userAgent == null || userAgent.isBlank() ? null : hashService.sha256(userAgent);
    }
}
