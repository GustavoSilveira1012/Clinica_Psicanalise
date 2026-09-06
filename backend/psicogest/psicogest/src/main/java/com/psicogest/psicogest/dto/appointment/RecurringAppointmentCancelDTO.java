package com.psicogest.psicogest.dto.appointment;

import com.psicogest.psicogest.model.enums.RecurrenceScope;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecurringAppointmentCancelDTO (
    @NotNull(
        message = "Escopo do cancelamento é obrigatório"
    )
    RecurrenceScope scope,

    @NotBlank(
        message = "Motivo do cancelamento é obrigatório"
    )
    @Size(
        max = 255,
        message = "Motivo do cancelamento não pode ter mais de 255 caracteres"
    )
    String reason
) {
    
}
