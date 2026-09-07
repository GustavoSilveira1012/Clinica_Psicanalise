package com.psicogest.psicogest.service;

import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.repository.*;
import com.psicogest.psicogest.security.jwt.JwtProperties;
import com.psicogest.psicogest.security.refresh.SecurityTokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class UserSessionService {
    private final UserSessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final SecurityTokenGenerator tokens;
    private final JwtProperties properties;
    private final Clock clock;
    public UserSessionService(UserSessionRepository sessions, RefreshTokenRepository refreshTokens,
            UserRepository users, SecurityTokenGenerator tokens, JwtProperties properties, Clock clock) {
        this.sessions = sessions; this.refreshTokens = refreshTokens; this.users = users;
        this.tokens = tokens; this.properties = properties; this.clock = clock;
    }

    public UserSession create(User user, String ip, String userAgent) {
        LocalDateTime now = LocalDateTime.now(clock);
        return sessions.saveAndFlush(UserSession.builder().id(UUID.randomUUID()).user(user)
                .createdAt(now).lastSeenAt(now).expiresAt(now.plus(properties.refreshTokenTtl()))
                .createdIp(ip).lastIp(ip).userAgentHash(userAgent == null ? null : tokens.hash(userAgent))
                .deviceLabel(deviceLabel(userAgent)).build());
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> list(Long userId, UUID current) {
        return sessions.findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastSeenAtDesc(
                userId, LocalDateTime.now(clock)).stream()
                .map(s -> new SessionResponse(s.getId(), s.getDeviceLabel(), s.getCreatedAt(),
                        s.getLastSeenAt(), s.getLastIp(), s.getId().equals(current))).toList();
    }

    public void revoke(Long userId, UUID id, String reason) {
        users.findByIdForUpdate(userId).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        UserSession session = sessions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada"));
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(LocalDateTime.now(clock));
            session.setRevocationReason(reason);
        }
        refreshTokens.revokeFamily(id, LocalDateTime.now(clock), reason);
    }

    // Caller holds the user row lock when changing account-wide security state.
    public void revokeAll(Long userId, String reason) {
        LocalDateTime now = LocalDateTime.now(clock);
        sessions.revokeAll(userId, now, reason);
        refreshTokens.revokeAllForUser(userId, now, reason);
    }

    private String deviceLabel(String ua) {
        if (ua == null) return "Dispositivo desconhecido";
        String browser = ua.contains("Edg/") ? "Edge" : ua.contains("Firefox/") ? "Firefox"
                : ua.contains("Chrome/") ? "Chrome" : ua.contains("Safari/") ? "Safari" : "Navegador";
        String os = ua.contains("Android") ? "Android" : ua.contains("iPhone") || ua.contains("iPad") ? "iOS"
                : ua.contains("Windows") ? "Windows" : ua.contains("Macintosh") ? "macOS"
                : ua.contains("Linux") ? "Linux" : "SO desconhecido";
        return browser + " / " + os;
    }

    public record SessionResponse(UUID id, String deviceLabel, LocalDateTime createdAt,
                                  LocalDateTime lastSeenAt, String lastIp, boolean current) {}
}
