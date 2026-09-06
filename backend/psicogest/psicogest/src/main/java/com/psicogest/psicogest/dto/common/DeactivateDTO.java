package com.psicogest.psicogest.dto.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeactivateDTO(

                @NotBlank(message = "Motivo é obrigatório")

                @Size(max = 255, message = "Motivo deve possuir no máximo 255 caracteres")

                String reason

) {
}