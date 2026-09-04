package com.psicogest.psicogest.dto.clinic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClinicCreateDTO(

        @NotBlank(message = "Nome da clínica é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String name,

        @Size(max = 18, message = "CNPJ deve ter no máximo 18 caracteres")
        String cnpj
) {
}