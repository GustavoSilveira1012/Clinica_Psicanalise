package com.psicogest.psicogest.dto.clinic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClinicMembershipPeriodEndDTO(

        @NotBlank(
                message = "Motivo do encerramento é obrigatório"
        )

        @Size(
                max = 255,
                message = "Motivo deve possuir no máximo 255 caracteres"
        )

        String reason

) {
}