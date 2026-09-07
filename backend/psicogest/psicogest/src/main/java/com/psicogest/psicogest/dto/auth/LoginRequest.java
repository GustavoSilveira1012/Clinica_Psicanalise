package com.psicogest.psicogest.dto.auth;

import jakarta.validation.constraints.*;

public record LoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(max = 200)
        String password

) {
}