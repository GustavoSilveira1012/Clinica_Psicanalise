package com.psicogest.psicogest.security;

import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.security.jwt.AccountStateJwtValidator;
import com.psicogest.psicogest.security.jwt.AudienceValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtValidatorTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void shouldAcceptOnlyTheConfiguredAudienceAndAccessTokenType() {
        AudienceValidator validator = new AudienceValidator("psicogest-web");

        assertThat(validator.validate(jwt(List.of("psicogest-web"), "access"))
                .hasErrors()).isFalse();
        assertThat(validator.validate(jwt(List.of("other-app"), "access"))
                .hasErrors()).isTrue();
        assertThat(validator.validate(jwt(List.of("psicogest-web"), "refresh"))
                .hasErrors()).isTrue();
    }

    @Test
    void shouldRejectJwtWhenTheAccountSecurityVersionChanged() {
        AccountStateJwtValidator validator = new AccountStateJwtValidator(userRepository);
        UserRepository.UserSecurityView view = userSecurityView(true, 2, null);
        when(userRepository.findProjectedById(eq(15L))).thenReturn(Optional.of(view));

        OAuth2TokenValidatorResult result = validator.validate(
                jwt(List.of("psicogest-web"), "access", 1));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void shouldAcceptJwtWhenTheAccountIsActiveAndTheVersionMatches() {
        AccountStateJwtValidator validator = new AccountStateJwtValidator(userRepository);
        UserRepository.UserSecurityView view = userSecurityView(true, 1, null);
        when(userRepository.findProjectedById(eq(15L))).thenReturn(Optional.of(view));

        OAuth2TokenValidatorResult result = validator.validate(
                jwt(List.of("psicogest-web"), "access", 1));

        assertThat(result.hasErrors()).isFalse();
    }

    private Jwt jwt(List<String> audience, String tokenType) {
        return jwt(audience, tokenType, 1);
    }

    private Jwt jwt(List<String> audience, String tokenType, int version) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .audience(audience)
                .subject("15")
                .claim("token_type", tokenType)
                .claim("sv", version)
                .build();
    }

    private UserRepository.UserSecurityView userSecurityView(
            Boolean active,
            Integer version,
            LocalDateTime lockedUntil
    ) {
        return new UserRepository.UserSecurityView() {
            @Override
            public Boolean getActive() {
                return active;
            }

            @Override
            public Integer getSecurityVersion() {
                return version;
            }

            @Override
            public LocalDateTime getLockedUntil() {
                return lockedUntil;
            }
        };
    }
}
