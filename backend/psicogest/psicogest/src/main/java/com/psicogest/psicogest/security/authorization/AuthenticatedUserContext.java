package com.psicogest.psicogest.security.authorization;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticatedUserContext {

    public Optional<Long> userId(
            Authentication authentication
    ) {

        if (
                authentication == null
                || !authentication.isAuthenticated()
        ) {
            return Optional.empty();
        }

        try {

            return Optional.of(
                    Long.valueOf(
                            authentication.getName()
                    )
            );

        } catch (
                NumberFormatException exception
        ) {

            return Optional.empty();
        }
    }

    public boolean hasRole(
            Authentication authentication,
            String role
    ) {

        if (authentication == null) {
            return false;
        }

        String expected =
                "ROLE_" + role;

        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(
                        authority ->
                                authority
                                        .getAuthority()
                                        .equals(expected)
                );
    }
}