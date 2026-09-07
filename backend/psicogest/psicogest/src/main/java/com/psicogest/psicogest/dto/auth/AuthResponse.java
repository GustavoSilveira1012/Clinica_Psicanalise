package com.psicogest.psicogest.dto.auth;

public record AuthResponse(

        String accessToken,

        String tokenType,

        long expiresIn,

        Long userId,

        String role

) {
}