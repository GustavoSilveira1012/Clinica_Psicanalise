package com.psicogest.psicogest.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthActionRequest(
        @NotBlank
        @Email
        @Size(max = 150)
        String email
) {
}
