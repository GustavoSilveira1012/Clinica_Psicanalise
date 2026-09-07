package com.psicogest.psicogest.service;

import com.psicogest.psicogest.exception.*;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.MfaMethodStatus;
import com.psicogest.psicogest.repository.*;
import com.psicogest.psicogest.security.jwt.JwtProperties;
import com.psicogest.psicogest.security.mfa.MfaPolicyService;
import com.psicogest.psicogest.security.refresh.SecurityTokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final SecurityTokenGenerator generator;
    private final JwtProperties properties;
    private final UserRepository users;
    private final UserSessionRepository sessions;
    private final UserSessionService sessionService;
    private final MfaMethodRepository methods;
    private final Clock clock;
    public RefreshTokenService(RefreshTokenRepository repository, SecurityTokenGenerator generator,
            JwtProperties properties, UserRepository users, UserSessionRepository sessions,
            UserSessionService sessionService, MfaMethodRepository methods, Clock clock) {
        this.repository = repository; this.generator = generator; this.properties = properties; this.users = users;
        this.sessions = sessions; this.sessionService = sessionService; this.methods = methods; this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issueInitial(User user, UUID sessionId, String ip, String userAgent) {
        return issue(user, sessionId, ip, userAgent);
    }

    private IssuedRefreshToken issue(User user, UUID familyId, String ip, String userAgent) {
        String raw = generator.generate();
        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken entity = repository.saveAndFlush(RefreshToken.builder().id(UUID.randomUUID()).user(user)
                .familyId(familyId).tokenHash(generator.hash(raw)).securityVersion(user.getSecurityVersion())
                .issuedAt(now).expiresAt(now.plus(properties.refreshTokenTtl())).createdIp(ip)
                .userAgentHash(userAgent == null ? null : generator.hash(userAgent)).build());
        return new IssuedRefreshToken(raw, entity);
    }

    @Transactional(noRollbackFor = RefreshTokenReuseDetectedException.class)
    public RotationResult rotate(String raw, String ip, String userAgent) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) throw new InvalidRefreshTokenException();
        String hash = generator.hash(raw);
        Long userId = repository.findUserIdByHash(hash).orElseThrow(InvalidRefreshTokenException::new);
        User user = users.findByIdForUpdate(userId).orElseThrow(InvalidRefreshTokenException::new);
        RefreshToken current = repository.findByTokenHashForUpdate(hash).orElseThrow(InvalidRefreshTokenException::new);
        LocalDateTime now = LocalDateTime.now(clock);
        if (current.getConsumedAt() != null) {
            sessionService.revoke(userId, current.getFamilyId(), "REFRESH_TOKEN_REUSE");
            throw new RefreshTokenReuseDetectedException();
        }
        UserSession session = sessions.findByIdAndUserId(current.getFamilyId(), userId)
                .orElseThrow(InvalidRefreshTokenException::new);
        if (current.getRevokedAt() != null || !now.isBefore(current.getExpiresAt())
                || session.getRevokedAt() != null || !now.isBefore(session.getExpiresAt())
                || !Boolean.TRUE.equals(user.getActive())
                || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))
                || !current.getSecurityVersion().equals(user.getSecurityVersion())
                || (MfaPolicyService.requiresMfa(user)
                    && !methods.existsByUserIdAndStatus(userId, MfaMethodStatus.ACTIVE))) {
            throw new InvalidRefreshTokenException();
        }
        var replacement = issue(user, current.getFamilyId(), ip, userAgent);
        current.setConsumedAt(now);
        current.setReplacedBy(replacement.entity());
        session.setLastSeenAt(now);
        session.setLastIp(ip);
        session.setExpiresAt(replacement.entity().getExpiresAt());
        return new RotationResult(user, replacement.rawToken(), session.getId());
    }

    @Transactional
    public void revokeCurrentSession(String raw) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) return;
        String hash = generator.hash(raw);
        repository.findUserIdByHash(hash).ifPresent(userId -> {
            users.findByIdForUpdate(userId).orElseThrow(InvalidRefreshTokenException::new);
            repository.findByTokenHashForUpdate(hash).ifPresent(token ->
                    sessionService.revoke(userId, token.getFamilyId(), "LOGOUT"));
        });
    }

    public record IssuedRefreshToken(String rawToken, RefreshToken entity) {}
    public record RotationResult(User user, String refreshToken, UUID sessionId) {}
}
