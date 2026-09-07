package com.psicogest.psicogest.security;
import com.psicogest.psicogest.dto.auth.*;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.*;
import com.psicogest.psicogest.repository.*;
import com.psicogest.psicogest.security.jwt.JwtService;
import com.psicogest.psicogest.security.mfa.ChallengeService;
import com.psicogest.psicogest.service.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Clock;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceSecurityTest {
    UserRepository users = mock(UserRepository.class);
    PasswordEncoder passwords = mock(PasswordEncoder.class);
    MfaMethodRepository methods = mock(MfaMethodRepository.class);
    ChallengeService challenges = mock(ChallengeService.class);
    AuthTokenService tokens = mock(AuthTokenService.class);
    RefreshTokenService refresh = mock(RefreshTokenService.class);
    UserSessionService sessions = mock(UserSessionService.class);
    JwtService jwt = mock(JwtService.class);
    AuthService service = new AuthService(users, passwords, methods, challenges, tokens, refresh, sessions, jwt, Clock.systemUTC());

    @Test void patientCanAuthenticateWithoutMfa() {
        User user = user(UserRole.PATIENT);
        when(users.findByEmailForUpdate("person@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("secret", "hash")).thenReturn(true);
        when(tokens.authenticate(user, "ip", "agent")).thenReturn(new AuthService.AuthTokens(
                new AuthResponse("access", "Bearer", 600, 15L, "PATIENT"), "refresh"));
        var result = service.login(new LoginRequest(" Person@Example.com ", "secret"), "ip", "agent");
        assertThat(result.response().status()).isEqualTo(LoginStatus.AUTHENTICATED);
        assertThat(result.response().authentication().accessToken()).isEqualTo("access");
        assertThat(result.response().toString()).doesNotContain("refresh");
    }

    @Test void professionalNeedsEnrollmentAndGetsNoTokens() {
        for (var role : new UserRole[]{UserRole.PSYCHOANALYST, UserRole.CLINIC_ADMIN, UserRole.SYSTEM_ADMIN}) {
            User user = user(role);
            when(users.findByEmailForUpdate("person@example.com")).thenReturn(Optional.of(user));
            when(passwords.matches("secret", "hash")).thenReturn(true);
            when(challenges.issue(user, AuthenticationChallengeType.MFA_ENROLLMENT_REQUIRED, "ip", "agent"))
                    .thenReturn("challenge");
            var result = service.login(new LoginRequest("person@example.com", "secret"), "ip", "agent");
            assertThat(result.response().status()).isEqualTo(LoginStatus.MFA_ENROLLMENT_REQUIRED);
            assertThat(result.response().authentication()).isNull();
            assertThat(result.refreshToken()).isNull();
        }
        verifyNoInteractions(tokens, jwt, refresh);
    }

    @Test void patientWithActiveMfaAlsoNeedsSecondFactor() {
        User user = user(UserRole.PATIENT);
        when(users.findByEmailForUpdate("person@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("secret", "hash")).thenReturn(true);
        when(methods.existsByUserIdAndStatus(15L, MfaMethodStatus.ACTIVE)).thenReturn(true);
        var result = service.login(new LoginRequest("person@example.com", "secret"), "ip", "agent");
        assertThat(result.response().status()).isEqualTo(LoginStatus.MFA_REQUIRED);
        verifyNoInteractions(tokens, jwt, refresh);
    }

    @Test void invalidPasswordNeverIssuesTokens() {
        User user = user(UserRole.PATIENT);
        when(users.findByEmailForUpdate("person@example.com")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.login(new LoginRequest("person@example.com", "wrong"), "ip", "agent"))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        verifyNoInteractions(tokens, jwt, refresh, challenges);
    }

    @Test void logoutAllIncrementsVersionAndRevokesSessions() {
        User user = user(UserRole.PATIENT);
        when(users.findByIdForUpdate(15L)).thenReturn(Optional.of(user));
        service.logoutAll(15L);
        assertThat(user.getSecurityVersion()).isEqualTo(2);
        verify(sessions).revokeAll(15L, "LOGOUT_ALL_DEVICES");
    }

    private User user(UserRole role) {
        return User.builder().id(15L).email("person@example.com").passwordHash("hash").role(role).active(true).build();
    }
}
