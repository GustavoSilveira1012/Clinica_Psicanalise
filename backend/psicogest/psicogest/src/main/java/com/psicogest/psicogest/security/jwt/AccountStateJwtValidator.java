package com.psicogest.psicogest.security.jwt;

import com.psicogest.psicogest.repository.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.UUID;

@Component
public class AccountStateJwtValidator implements OAuth2TokenValidator<Jwt> {
    private final UserRepository users;
    private final UserSessionRepository sessions;
    private final Clock clock;
    public AccountStateJwtValidator(UserRepository users, UserSessionRepository sessions, Clock clock) {
        this.users = users; this.sessions = sessions; this.clock = clock;
    }
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            Long userId = Long.valueOf(jwt.getSubject());
            Object version = jwt.getClaims().get("sv");
            if (!(version instanceof Number tokenVersion)) return failure();
            var user = users.findProjectedById(userId).orElse(null);
            LocalDateTime now = LocalDateTime.now(clock);
            if (user == null || !Boolean.TRUE.equals(user.getActive())
                    || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))
                    || user.getSecurityVersion() == null
                    || !user.getSecurityVersion().toString().equals(tokenVersion.toString())) return failure();
            UUID sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
            if (!sessions.existsByIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(sessionId, userId, now)) return failure();
            return OAuth2TokenValidatorResult.success();
        } catch (Exception exception) {
            return failure();
        }
    }
    private OAuth2TokenValidatorResult failure() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token inválido", null));
    }
}
