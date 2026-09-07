package com.psicogest.psicogest.dto.auth;

public record CsrfResponse(
        String headerName,
        String token
) {
}
