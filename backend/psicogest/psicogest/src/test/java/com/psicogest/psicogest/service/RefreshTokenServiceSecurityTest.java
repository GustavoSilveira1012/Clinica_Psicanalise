package com.psicogest.psicogest.service;

import com.psicogest.psicogest.exception.RefreshTokenReuseDetectedException;
import com.psicogest.psicogest.model.entity.RefreshToken;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.entity.UserSession;
import com.psicogest.psicogest.repository.*;
import com.psicogest.psicogest.security.auth.mfa.MfaProperties;
import com.psicogest.psicogest.security.auth.refresh.SecurityTokenGenerator;
import com.psicogest.psicogest.security.auth.jwt.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceSecurityTest {

    @Mock private RefreshTokenRepository repository;
    @Mock private UserRepository users;
    @Mock private UserSessionRepository sessions;
    @Mock private UserSessionService sessionService;
    @Mock private MfaMethodRepository methods;
    @org.mockito.Spy private SecurityTokenGenerator generator = new SecurityTokenGenerator();
    @org.mockito.Spy private Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @InjectMocks private RefreshTokenService service;

    @Test
    void refreshTokenReuseRevokesTheWholeSessionFamily() {
        String raw = generator.generate();
        UUID familyId = UUID.randomUUID();
        User user = User.builder().id(41L).active(true).securityVersion(3).build();
        RefreshToken consumed = RefreshToken.builder()
                .id(UUID.randomUUID()).user(user).familyId(familyId)
                .tokenHash(generator.hash(raw)).securityVersion(3)
                .issuedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(1))
                .consumedAt(LocalDateTime.now()).build();

        when(repository.findUserIdByHash(generator.hash(raw))).thenReturn(Optional.of(41L));
        when(users.findByIdForUpdate(41L)).thenReturn(Optional.of(user));
        when(repository.findByTokenHashForUpdate(generator.hash(raw))).thenReturn(Optional.of(consumed));

        assertThatThrownBy(() -> service.rotate(raw, "127.0.0.1", "ua"))
                .isInstanceOf(RefreshTokenReuseDetectedException.class);

        verify(sessionService).revoke(41L, familyId, "REFRESH_TOKEN_REUSE");
        verifyNoInteractions(sessions, methods);
    }
}
