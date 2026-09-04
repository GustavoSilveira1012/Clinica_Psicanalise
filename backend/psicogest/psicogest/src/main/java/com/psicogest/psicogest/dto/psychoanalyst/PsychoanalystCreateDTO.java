package com.psicogest.psicogest.dto.psychoanalyst;

import jakarta.validation.constraints.*;

public record PsychoanalystCreateDTO(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150)
        String name,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String password,

        @Size(max = 100)
        String licenseNumber,

        @Size(max = 150)
        String specialization,

        String bio,

        @Size(max = 30)
        String phone
) {
}