package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.auth.AuthResponse;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.security.jwt.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service
public class AuthTokenService {
    private final UserSessionService sessions;
    private final RefreshTokenService refresh;
    private final JwtService jwt;
    private final Clock clock;
    public AuthTokenService(UserSessionService sessions, RefreshTokenService refresh, JwtService jwt, Clock clock) {
        this.sessions = sessions; this.refresh = refresh; this.jwt = jwt; this.clock = clock;
    }

    @Transactional
    public AuthService.AuthTokens authenticate(User user, String ip, String userAgent) {
        UserSession session = sessions.create(user, ip, userAgent);
        var token = refresh.issueInitial(user, session.getId(), ip, userAgent);
        var access = jwt.issueAccessToken(user, session.getId());
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now(clock));
        return new AuthService.AuthTokens(response(user, access), token.rawToken());
    }

    public AuthResponse response(User user, JwtService.AccessToken access) {
        return new AuthResponse(access.value(), "Bearer",
                Math.max(0, Duration.between(clock.instant(), access.expiresAt()).toSeconds()),
                user.getId(), user.getRole().name());
    }
}
