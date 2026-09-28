package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.entity.UserSession;
import com.psicogest.psicogest.repository.RefreshTokenRepository;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.repository.UserSessionRepository;
import com.psicogest.psicogest.security.auth.jwt.JwtProperties;
import com.psicogest.psicogest.security.auth.refresh.SecurityTokenGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceSecurityTest {

    @Mock private UserSessionRepository sessions;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private UserRepository users;
    @Mock private com.psicogest.psicogest.security.auth.jwt.JwtProperties properties;
    @org.mockito.Spy private SecurityTokenGenerator tokens = new SecurityTokenGenerator();
    @org.mockito.Spy private java.time.Clock clock = java.time.Clock.fixed(
            Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @InjectMocks private UserSessionService service;

    @Test
    void revokingOneSessionRevokesItsRefreshFamily() {
        UUID sessionId = UUID.randomUUID();
        User user = User.builder().id(41L).build();
        UserSession session = UserSession.builder().id(sessionId).user(user).build();
        when(users.findByIdForUpdate(41L)).thenReturn(Optional.of(user));
        when(sessions.findByIdAndUserId(sessionId, 41L)).thenReturn(Optional.of(session));

        service.revoke(41L, sessionId, "USER_REVOKED");

        verify(refreshTokens).revokeFamily(eq(sessionId), any(LocalDateTime.class), eq("USER_REVOKED"));
        verify(users).findByIdForUpdate(41L);
    }

    @Test
    void logoutAllRevokesEverySessionAndRefreshToken() {
        service.revokeAll(41L, "LOGOUT_ALL_DEVICES");

        verify(sessions).revokeAll(eq(41L), any(LocalDateTime.class), eq("LOGOUT_ALL_DEVICES"));
        verify(refreshTokens).revokeAllForUser(eq(41L), any(LocalDateTime.class), eq("LOGOUT_ALL_DEVICES"));
    }
}
