package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.auth.EmailVerificationRequest;
import com.psicogest.psicogest.dto.auth.AuthActionRequest;
import com.psicogest.psicogest.dto.auth.AuthActionRequestResponse;
import com.psicogest.psicogest.dto.auth.PasswordResetCompletionRequest;
import com.psicogest.psicogest.service.AuthActionDeliveryException;
import com.psicogest.psicogest.service.AuthActionRequestService;
import com.psicogest.psicogest.service.AuthActionTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Public token-completion endpoints. Request/delivery endpoints stay disabled until a real mail provider is configured. */
@RestController
@RequestMapping("/auth")
public class AuthActionController {

    private static final String REQUEST_ACCEPTED_MESSAGE =
            "Se o endereço estiver cadastrado, você receberá as instruções por e-mail.";

    private final AuthActionTokenService actionTokens;
    private final AuthActionRequestService actionRequests;

    public AuthActionController(AuthActionTokenService actionTokens, AuthActionRequestService actionRequests) {
        this.actionTokens = actionTokens;
        this.actionRequests = actionRequests;
    }

    @PostMapping("/password/reset-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthActionRequestResponse requestPasswordReset(
            @Valid @RequestBody AuthActionRequest request,
            HttpServletRequest servletRequest
    ) {
        try {
            actionRequests.requestPasswordReset(request.email(), servletRequest.getRemoteAddr());
        } catch (AuthActionDeliveryException ignored) {
            // Same public response whether the address exists or provider delivery failed.
        }
        return new AuthActionRequestResponse(REQUEST_ACCEPTED_MESSAGE);
    }

    @PostMapping("/email/verification-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthActionRequestResponse requestEmailVerification(
            @Valid @RequestBody AuthActionRequest request,
            HttpServletRequest servletRequest
    ) {
        try {
            actionRequests.requestEmailVerification(request.email(), servletRequest.getRemoteAddr());
        } catch (AuthActionDeliveryException ignored) {
            // Same public response whether the address exists or provider delivery failed.
        }
        return new AuthActionRequestResponse(REQUEST_ACCEPTED_MESSAGE);
    }

    @PostMapping("/email/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        if (!actionTokens.verifyEmail(request.token())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou expirado");
        }
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody PasswordResetCompletionRequest request) {
        if (!actionTokens.resetPassword(request.token(), request.newPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Token inválido/expirado ou senha fora da política de segurança");
        }
    }
}
