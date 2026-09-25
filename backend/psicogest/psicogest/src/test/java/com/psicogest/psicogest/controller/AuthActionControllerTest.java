package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.auth.EmailVerificationRequest;
import com.psicogest.psicogest.dto.auth.AuthActionRequest;
import com.psicogest.psicogest.service.AuthActionRequestService;
import com.psicogest.psicogest.dto.auth.PasswordResetCompletionRequest;
import com.psicogest.psicogest.service.AuthActionTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthActionControllerTest {

    private static final String TOKEN = "a".repeat(43);

    private final AuthActionTokenService tokens = mock(AuthActionTokenService.class);
    private final AuthActionRequestService requests = mock(AuthActionRequestService.class);
    private final AuthActionController controller = new AuthActionController(tokens, requests);

    @Test
    void delegatesEmailVerificationWithoutReturningTokenOrAccountDetails() {
        when(tokens.verifyEmail(TOKEN)).thenReturn(true);

        controller.verifyEmail(new EmailVerificationRequest(TOKEN));

        verify(tokens).verifyEmail(TOKEN);
    }

    @Test
    void rejectsInvalidEmailVerificationTokenWithGenericClientError() {
        when(tokens.verifyEmail(TOKEN)).thenReturn(false);

        assertThatThrownBy(() -> controller.verifyEmail(new EmailVerificationRequest(TOKEN)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Token inválido ou expirado");
    }

    @Test
    void delegatesPasswordResetAndDoesNotLogOrEchoCredentials() {
        String password = "synthetic strong password 2026";
        when(tokens.resetPassword(TOKEN, password)).thenReturn(true);

        controller.resetPassword(new PasswordResetCompletionRequest(TOKEN, password));

        verify(tokens).resetPassword(TOKEN, password);
    }

    @Test
    void rejectsInvalidPasswordResetWithoutRevealingAccountState() {
        String password = "synthetic strong password 2026";
        when(tokens.resetPassword(TOKEN, password)).thenReturn(false);

        assertThatThrownBy(() -> controller.resetPassword(new PasswordResetCompletionRequest(TOKEN, password)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Token inválido/expirado ou senha fora da política de segurança");
    }

    @Test
    void returnsSameGenericRecoveryResponseAndUsesOnlyRequestIpAsRateLimitInput() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRemoteAddr()).thenReturn("192.0.2.10");

        var response = controller.requestPasswordReset(
                new AuthActionRequest("user@example.invalid"), servletRequest);

        verify(requests).requestPasswordReset("user@example.invalid", "192.0.2.10");
        org.assertj.core.api.Assertions.assertThat(response.message())
                .isEqualTo("Se o endereço estiver cadastrado, você receberá as instruções por e-mail.");
    }

    @Test
    void returnsSameGenericVerificationResponseWhenDeliveryFails() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRemoteAddr()).thenReturn("192.0.2.10");
        org.mockito.Mockito.doThrow(new com.psicogest.psicogest.service.AuthActionDeliveryException())
                .when(requests).requestEmailVerification("user@example.invalid", "192.0.2.10");

        var response = controller.requestEmailVerification(
                new AuthActionRequest("user@example.invalid"), servletRequest);

        org.assertj.core.api.Assertions.assertThat(response.message())
                .isEqualTo("Se o endereço estiver cadastrado, você receberá as instruções por e-mail.");
    }
}
