package com.psicogest.psicogest.security.jwt;

import com.psicogest.psicogest.repository.UserRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AccountStateJwtValidator implements OAuth2TokenValidator<Jwt> {

    private final UserRepository userRepository;

    public AccountStateJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            Long userId = Long.valueOf(jwt.getSubject());
            Object versionClaim = jwt.getClaims().get("sv");

            if (!(versionClaim instanceof Number tokenVersion)) {
                return failure();
            }

            UserRepository.UserSecurityView user = userRepository
                    .findProjectedById(userId)
                    .orElse(null);

            if (user == null
                    || Boolean.FALSE.equals(user.getActive())
                    || (user.getLockedUntil() != null
                    && user.getLockedUntil().isAfter(LocalDateTime.now()))
                    || user.getSecurityVersion() == null
                    || user.getSecurityVersion().intValue() != tokenVersion.intValue()) {
                return failure();
            }

            return OAuth2TokenValidatorResult.success();
        } catch (Exception exception) {
            return failure();
        }
    }

    private OAuth2TokenValidatorResult failure() {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Token inválido", null));
    }
}
