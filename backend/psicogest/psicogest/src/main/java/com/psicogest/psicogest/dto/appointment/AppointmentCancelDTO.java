package com.psicogest.psicogest.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppointmentCancelDTO(

                @NotBlank(message = "Motivo do cancelamento é obrigatório")

                @Size(max = 255, message = "Motivo deve possuir no máximo 255 caracteres") String reason

) {
}