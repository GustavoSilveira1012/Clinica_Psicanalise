package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.AuthActionToken;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.AuthActionTokenType;
import com.psicogest.psicogest.repository.AuthActionTokenRepository;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.security.auth.refresh.SecurityTokenGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthActionTokenServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 12, 0);
    private static final String RAW_TOKEN = "a".repeat(43);
    private static final String TOKEN_HASH = "c".repeat(64);

    private AuthActionTokenRepository tokens;
    private UserRepository users;
    private SecurityTokenGenerator generator;
    private PasswordEncoder passwordEncoder;
    private UserSessionService sessions;
    private AuthActionTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        tokens = mock(AuthActionTokenRepository.class);
        users = mock(UserRepository.class);
        generator = mock(SecurityTokenGenerator.class);
        passwordEncoder = mock(PasswordEncoder.class);
        sessions = mock(UserSessionService.class);
        service = new AuthActionTokenService(tokens, users, generator, passwordEncoder, sessions,
                Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"), ZoneOffset.UTC));
        user = User.builder().id(41L).active(true).securityVersion(3).build();
    }

    @Test
    void persistsOnlyTheHashAndInvalidatesEarlierTokensOfTheSamePurpose() {
        when(users.findByIdForUpdate(41L)).thenReturn(Optional.of(user));
        when(generator.generate()).thenReturn(RAW_TOKEN);
        when(generator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);

        String deliveredOnlyToCaller = service.issue(41L, AuthActionTokenType.PASSWORD_RESET);

        ArgumentCaptor<AuthActionToken> stored = ArgumentCaptor.forClass(AuthActionToken.class);
        verify(tokens).invalidatePending(41L, AuthActionTokenType.PASSWORD_RESET, NOW);
        verify(tokens).save(stored.capture());
        assertThat(deliveredOnlyToCaller).isEqualTo(RAW_TOKEN);
        assertThat(stored.getValue().getTokenHash()).isEqualTo(TOKEN_HASH).isNotEqualTo(RAW_TOKEN);
        assertThat(stored.getValue().getExpiresAt()).isEqualTo(NOW.plusMinutes(20));
        assertThat(stored.getValue().getSecurityVersion()).isEqualTo(3);
    }

    @Test
    void consumesMatchingUnexpiredTokenOnce() {
        AuthActionToken token = token(AuthActionTokenType.EMAIL_VERIFICATION, NOW.plusHours(1));
        when(generator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(tokens.findUserIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(41L));
        when(users.findByIdForUpdate(41L)).thenReturn(Optional.of(user));
        when(tokens.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(token));

        AtomicReference<User> completedUser = new AtomicReference<>();
        assertThat(service.consume(RAW_TOKEN, AuthActionTokenType.EMAIL_VERIFICATION, completedUser::set)).isTrue();
        assertThat(completedUser.get()).isSameAs(user);
        assertThat(token.getConsumedAt()).isEqualTo(NOW);
        assertThat(service.consume(RAW_TOKEN, AuthActionTokenType.EMAIL_VERIFICATION,
                ignored -> { throw new AssertionError("replay callback must not run"); })).isFalse();
        verify(tokens, times(2)).findByTokenHashForUpdate(TOKEN_HASH);
    }

    @Test
    void verifiesEmailAndConsumesTheActionTokenOnce() {
        AuthActionToken token = token(AuthActionTokenType.EMAIL_VERIFICATION, NOW.plusHours(1));
        when(generator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(tokens.findUserIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(41L));
        when(users.findByIdForUpdate(41L)).thenReturn(Optional.of(user));
        when(tokens.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(token));

        assertThat(service.verifyEmail(RAW_TOKEN)).isTrue();
        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
        assertThat(token.getConsumedAt()).isEqualTo(NOW);
        assertThat(service.verifyEmail(RAW_TOKEN)).isFalse();
    }

    @Test
    void rejectsExpiredWrongPurposeAndStaleSecurityVersionTokens() {
        AuthActionToken expired = token(AuthActionTokenType.PASSWORD_RESET, NOW);
        AuthActionToken wrongPurpose = token(AuthActionTokenType.EMAIL_VERIFICATION, NOW.plusHours(1));
        AuthActionToken stale = token(AuthActionTokenType.PASSWORD_RESET, NOW.plusHours(1));
        stale.setSecurityVersion(2);
        when(generator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(tokens.findUserIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(41L));
        when(users.findByIdForUpdate(41L)).thenReturn(Optional.of(user));
        when(tokens.findByTokenHashForUpdate(TOKEN_HASH))
                .thenReturn(Optional.of(expired), Optional.of(wrongPurpose), Optional.of(stale));

        assertThat(service.consume(RAW_TOKEN, AuthActionTokenType.PASSWORD_RESET,
                ignored -> { throw new AssertionError("expired callback must not run"); })).isFalse();
        assertThat(service.consume(RAW_TOKEN, AuthActionTokenType.PASSWORD_RESET,
                ignored -> { throw new AssertionError("wrong-purpose callback must not run"); })).isFalse();
        assertThat(service.consume(RAW_TOKEN, AuthActionTokenType.PASSWORD_RESET,
                ignored -> { throw new AssertionError("stale callback must not run"); })).isFalse();
        assertThat(expired.getConsumedAt()).isNull();
        assertThat(wrongPurpose.getConsumedAt()).isNull();
        assertThat(stale.getConsumedAt()).isNull();
    }

    @Test
    void rejectsMalformedTokensWithoutQueryingStorage() {
        assertThat(service.consume("not-a-token", AuthActionTokenType.PASSWORD_RESET,
                ignored -> { throw new AssertionError("malformed callback must not run"); })).isFalse();
        verifyNoInteractions(tokens, generator);
    }

    @Test
    void passwordResetConsumesTokenChangesCredentialAndRevokesAllSessionsAtomically() {
        String newPassword = "strong synthetic password 2026";
        AuthActionToken token = token(AuthActionTokenType.PASSWORD_RESET, NOW.plusMinutes(1));
        user.setFailedLoginAttempts(5);
        user.setLockedUntil(NOW.plusMinutes(10));
        user.setLastFailedLoginAt(NOW.minusMinutes(1));
        user.setRequirePasswordChange(true);
        when(generator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(tokens.findUserIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(41L));
        when(users.findByIdForUpdate(41L)).thenReturn(Optional.of(user));
        when(tokens.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(token));
        when(passwordEncoder.encode(newPassword)).thenReturn("bcrypt-reset-hash");

        assertThat(service.resetPassword(RAW_TOKEN, newPassword)).isTrue();

        assertThat(token.getConsumedAt()).isEqualTo(NOW);
        assertThat(user.getPasswordHash()).isEqualTo("bcrypt-reset-hash");
        assertThat(user.getPasswordChangedAt()).isEqualTo(NOW);
        assertThat(user.getSecurityVersion()).isEqualTo(4);
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLastFailedLoginAt()).isNull();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getRequirePasswordChange()).isFalse();
        verify(sessions).revokeAll(41L, "PASSWORD_RESET");
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    void rejectsWeakOrBcryptTruncatingPasswordsBeforeReadingToken() {
        assertThat(service.resetPassword(RAW_TOKEN, "Short12345")).isFalse();
        assertThat(service.resetPassword(RAW_TOKEN, "a".repeat(73))).isFalse();
        assertThat(service.resetPassword(RAW_TOKEN, "😀".repeat(19))).isFalse();

        verifyNoInteractions(tokens, generator, passwordEncoder, sessions);
    }

    @Test
    void refusesPasswordResetWithInvalidTokenAndDoesNotChangeSessions() {
        when(generator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(tokens.findUserIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThat(service.resetPassword(RAW_TOKEN, "strong synthetic password 2026")).isFalse();

        verifyNoInteractions(sessions);
        verify(passwordEncoder, never()).encode(anyString());
    }

    private AuthActionToken token(AuthActionTokenType type, LocalDateTime expiresAt) {
        return AuthActionToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(TOKEN_HASH)
                .tokenType(type)
                .securityVersion(3)
                .expiresAt(expiresAt)
                .createdAt(NOW.minusMinutes(1))
                .build();
    }
}
