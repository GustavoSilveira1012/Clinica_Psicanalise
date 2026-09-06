package com.psicogest.psicogest.dto.appointment;

import com.psicogest.psicogest.model.enums.RecurrenceScope;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RecurringAppointmentRescheduleDTO (
    @NotNull
    RecurrenceScope scope,

    @NotNull
    LocalDateTime scheduledStart,

    @NotNull
    LocalDateTime scheduledEnd
){
    
}
