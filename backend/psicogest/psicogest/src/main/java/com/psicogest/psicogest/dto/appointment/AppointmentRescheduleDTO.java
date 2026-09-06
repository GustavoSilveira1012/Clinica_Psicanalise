package com.psicogest.psicogest.dto.appointment;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentRescheduleDTO(

        @NotNull
        LocalDateTime scheduledStart,

        @NotNull
        LocalDateTime scheduledEnd

) {
}