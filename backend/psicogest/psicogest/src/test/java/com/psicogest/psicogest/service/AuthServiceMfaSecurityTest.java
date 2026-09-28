package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.auth.LoginRequest;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.AuthenticationChallengeType;
import com.psicogest.psicogest.model.enums.MfaMethodStatus;
import com.psicogest.psicogest.repository.MfaMethodRepository;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.security.auth.jwt.JwtService;
import com.psicogest.psicogest.security.auth.mfa.ChallengeService;
import com.psicogest.psicogest.security.event.SecurityEventService;
import com.psicogest.psicogest.security.request.SecurityRequestContext;
import com.psicogest.psicogest.security.request.SecurityRequestContextFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceMfaSecurityTest {

    @Mock private UserRepository users;
    @Mock private PasswordEncoder passwords;
    @Mock private MfaMethodRepository methods;
    @Mock private ChallengeService challenges;
    @Mock private AuthTokenService tokens;
    @Mock private RefreshTokenService refreshTokens;
    @Mock private UserSessionService sessions;
    @Mock private JwtService jwt;
    @Mock private LoginRateLimitService loginRateLimitService;
    @Mock private BruteForceProtectionService bruteForceProtectionService;
    @Mock private SecurityRequestContextFactory contextFactory;
    @Mock private SecurityEventService securityEventService;
    @org.mockito.Spy private Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @InjectMocks private AuthService service;

    @Test
    void passwordAuthenticationWithActiveMfaNeverIssuesAccessOrRefreshTokens() {
        User user = User.builder().id(41L).active(true).securityVersion(1).build();
        SecurityRequestContext context = new SecurityRequestContext("127.0.0.1", "ua", "POST", "/auth/login");
        when(passwords.matches("secret", "stored")).thenReturn(true);
        user.setPasswordHash("stored");
        when(users.findByEmailForUpdate("person@example.com")).thenReturn(Optional.of(user));
        when(methods.existsByUserIdAndStatus(41L, MfaMethodStatus.ACTIVE)).thenReturn(true);
        when(challenges.issue(user, AuthenticationChallengeType.MFA_REQUIRED, "127.0.0.1", "ua"))
                .thenReturn("challenge");

        AuthService.LoginResult result = service.login(
                new LoginRequest("person@example.com", "secret"), context);

        assertThat(result.response().status().name()).isEqualTo("MFA_REQUIRED");
        assertThat(result.response().challenge()).isEqualTo("challenge");
        assertThat(result.refreshToken()).isNull();
        verifyNoInteractions(tokens, refreshTokens, sessions, jwt);
    }
}
