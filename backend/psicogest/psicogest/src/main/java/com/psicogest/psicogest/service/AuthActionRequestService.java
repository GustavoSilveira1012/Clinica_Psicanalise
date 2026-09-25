package com.psicogest.psicogest.service;

import com.psicogest.psicogest.config.AuthActionMailProperties;
import com.psicogest.psicogest.infrastructure.redis.AuthActionRequestRateLimiter;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.AuthActionTokenType;
import com.psicogest.psicogest.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthActionRequestService {

    private final AuthActionTokenService tokens;
    private final UserRepository users;
    private final AuthActionMailProvider mailProvider;
    private final AuthActionMailProperties mailProperties;
    private final AuthActionRequestRateLimiter rateLimiter;

    public AuthActionRequestService(
            AuthActionTokenService tokens,
            UserRepository users,
            AuthActionMailProvider mailProvider,
            AuthActionMailProperties mailProperties,
            AuthActionRequestRateLimiter rateLimiter
    ) {
        this.tokens = tokens;
        this.users = users;
        this.mailProvider = mailProvider;
        this.mailProperties = mailProperties;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public void requestPasswordReset(String email, String remoteAddress) {
        request(email, remoteAddress, AuthActionTokenType.PASSWORD_RESET);
    }

    @Transactional
    public void requestEmailVerification(String email, String remoteAddress) {
        request(email, remoteAddress, AuthActionTokenType.EMAIL_VERIFICATION);
    }

    public boolean isAvailable() {
        return mailProvider.isAvailable();
    }

    private void request(String email, String remoteAddress, AuthActionTokenType type) {
        if (!mailProvider.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Solicitações por e-mail estão temporariamente indisponíveis");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!rateLimiter.allow(remoteAddress, normalizedEmail, type)) {
            return;
        }

        User user = users.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            return;
        }

        String rawToken;
        try {
            rawToken = tokens.issue(user.getId(), type);
        } catch (IllegalArgumentException accountBecameUnavailable) {
            // The public response is intentionally identical for unknown, inactive, and concurrently disabled accounts.
            return;
        }

        String path = type == AuthActionTokenType.PASSWORD_RESET ? "/reset-password" : "/verify-email";
        String link = mailProperties.publicBaseUrl().replaceAll("/+$", "") + path + "#token=" + rawToken;
        String subject = type == AuthActionTokenType.PASSWORD_RESET
                ? "Redefinição de senha — PsicoGest"
                : "Confirme seu e-mail — PsicoGest";
        String action = type == AuthActionTokenType.PASSWORD_RESET
                ? "Para escolher uma nova senha, abra este link:"
                : "Para confirmar seu endereço de e-mail, abra este link:";
        String body = action + "\n\n" + link
                + "\n\nO link é de uso único e expira em "
                + (type == AuthActionTokenType.PASSWORD_RESET ? "20 minutos." : "24 horas.")
                + " Se você não solicitou esta ação, ignore esta mensagem.";

        try {
            mailProvider.send(normalizedEmail, subject, body);
        } catch (RuntimeException deliveryFailure) {
            // Do not persist a usable token when delivery failed. Exception details can contain recipient data.
            throw new AuthActionDeliveryException();
        }
    }
}
