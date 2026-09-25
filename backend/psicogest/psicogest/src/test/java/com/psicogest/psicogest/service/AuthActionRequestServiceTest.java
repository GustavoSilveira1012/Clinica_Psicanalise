package com.psicogest.psicogest.service;

import com.psicogest.psicogest.config.AuthActionMailProperties;
import com.psicogest.psicogest.infrastructure.redis.AuthActionRequestRateLimiter;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.AuthActionTokenType;
import com.psicogest.psicogest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthActionRequestServiceTest {

    private static final String EMAIL = "user@example.invalid";
    private static final String IP = "192.0.2.20";
    private static final String TOKEN = "a".repeat(43);

    private final AuthActionTokenService tokens = mock(AuthActionTokenService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AuthActionMailProvider provider = mock(AuthActionMailProvider.class);
    private final AuthActionRequestRateLimiter limiter = mock(AuthActionRequestRateLimiter.class);
    private AuthActionRequestService service;

    @BeforeEach
    void setUp() {
        when(provider.isAvailable()).thenReturn(true);
        when(limiter.allow(IP, EMAIL, AuthActionTokenType.PASSWORD_RESET)).thenReturn(true);
        when(limiter.allow(IP, EMAIL, AuthActionTokenType.EMAIL_VERIFICATION)).thenReturn(true);
        service = new AuthActionRequestService(tokens, users, provider,
                enabledMailProperties(), limiter);
    }

    @Test
    void sendsResetLinkOnlyForActiveAccountAndUsesFragmentForToken() {
        User user = User.builder().id(11L).email(EMAIL).active(true).build();
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(tokens.issue(11L, AuthActionTokenType.PASSWORD_RESET)).thenReturn(TOKEN);

        service.requestPasswordReset("  USER@example.invalid ", IP);

        verify(tokens).issue(11L, AuthActionTokenType.PASSWORD_RESET);
        verify(provider).send(eq(EMAIL), contains("Redefinição de senha"), contains(
                "https://app.example.invalid/reset-password#token=" + TOKEN));
    }

    @Test
    void unknownOrInactiveAccountsCauseNoEmailOrTokenAndDoNotThrow() {
        User inactive = User.builder().id(12L).email(EMAIL).active(false).build();
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty(), Optional.of(inactive));

        service.requestPasswordReset(EMAIL, IP);
        service.requestPasswordReset(EMAIL, IP);

        verifyNoInteractions(tokens);
        verify(provider, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void emailDeliveryFailureThrowsGenericExceptionSoIssuedTokenTransactionRollsBack() {
        User user = User.builder().id(11L).email(EMAIL).active(true).build();
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(tokens.issue(11L, AuthActionTokenType.EMAIL_VERIFICATION)).thenReturn(TOKEN);
        doThrow(new IllegalStateException("SMTP response includes sensitive recipient data"))
                .when(provider).send(eq(EMAIL), anyString(), anyString());

        assertThatThrownBy(() -> service.requestEmailVerification(EMAIL, IP))
                .isInstanceOf(AuthActionDeliveryException.class)
                .hasMessage("Não foi possível entregar a mensagem de ação de conta");
    }

    @Test
    void rateLimitedRequestsDoNotLookUpAccountsOrCreateTokens() {
        when(limiter.allow(IP, EMAIL, AuthActionTokenType.PASSWORD_RESET)).thenReturn(false);

        service.requestPasswordReset(EMAIL, IP);

        verifyNoInteractions(users, tokens);
        verify(provider, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void disabledMailProviderRejectsAllRequestsBeforeLookingUpAddress() {
        when(provider.isAvailable()).thenReturn(false);

        assertThatThrownBy(() -> service.requestPasswordReset(EMAIL, IP))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verifyNoInteractions(users, tokens, limiter);
    }

    private AuthActionMailProperties enabledMailProperties() {
        return new AuthActionMailProperties(true, "no-reply@example.invalid",
                "https://app.example.invalid", Base64.getEncoder().encodeToString(new byte[32]),
                10, 3, Duration.ofHours(1));
    }
}
