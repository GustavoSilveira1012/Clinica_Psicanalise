package com.psicogest.psicogest.security;

import com.psicogest.psicogest.dto.auth.LoginRequest;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.UserRole;
import com.psicogest.psicogest.security.jwt.JwtProperties;
import com.psicogest.psicogest.security.jwt.JwtService;
import com.psicogest.psicogest.service.AuthService;
import com.psicogest.psicogest.service.RefreshTokenService;
import com.psicogest.psicogest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("dummy-password-never-used"))
                .thenReturn("dummy-hash");

        JwtProperties properties = new JwtProperties(
                "psicogest-api",
                "psicogest-web",
                Duration.ofMinutes(10),
                Duration.ofDays(14),
                null,
                null,
                "test",
                "psicogest_rt",
                false);

        service = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                properties);
    }

    @Test
    void shouldReturnAccessTokenAndKeepRefreshTokenOutOfResponse() {
        User user = user();
        when(userRepository.findByEmailIgnoreCase("person@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "stored-hash"))
                .thenReturn(true);

        JwtService.AccessToken access = new JwtService.AccessToken(
                "access-jwt",
                Instant.now().plusSeconds(600));
        when(jwtService.issueAccessToken(user)).thenReturn(access);

        RefreshTokenService.IssuedRefreshToken refresh =
                new RefreshTokenService.IssuedRefreshToken("refresh-secret", null);
        when(refreshTokenService.issueInitial(user, "127.0.0.1", "agent"))
                .thenReturn(refresh);

        AuthService.AuthTokens result = service.login(
                new LoginRequest(" Person@Example.com ", "secret"),
                "127.0.0.1",
                "agent");

        assertThat(result.response().accessToken()).isEqualTo("access-jwt");
        assertThat(result.refreshToken()).isEqualTo("refresh-secret");
        assertThat(result.response().toString()).doesNotContain("refresh-secret");
    }

    @Test
    void shouldRejectInvalidPasswordWithoutIssuingTokens() {
        User user = user();
        when(userRepository.findByEmailIgnoreCase("person@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.login(
                new LoginRequest("person@example.com", "wrong"),
                "127.0.0.1",
                "agent"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciais inválidas");

        verify(jwtService, never()).issueAccessToken(any());
        verify(refreshTokenService, never()).issueInitial(any(), any(), any());
    }

    @Test
    void shouldIncrementSecurityVersionAndRevokeAllRefreshTokens() {
        User user = user();
        user.setSecurityVersion(4);
        when(userRepository.findById(15L)).thenReturn(Optional.of(user));

        service.logoutAll(15L);

        assertThat(user.getSecurityVersion()).isEqualTo(5);
        verify(userRepository).saveAndFlush(user);
        verify(refreshTokenService).revokeAllForUser(eq(15L));
    }

    private User user() {
        User user = new User();
        user.setId(15L);
        user.setEmail("person@example.com");
        user.setPasswordHash("stored-hash");
        user.setRole(UserRole.PSYCHOANALYST);
        user.setActive(true);
        user.setSecurityVersion(1);
        user.setFailedLoginAttempts(0);
        return user;
    }
}
