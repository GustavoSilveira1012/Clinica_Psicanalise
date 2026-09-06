package com.psicogest.psicogest.dto.appointment;

import com.psicogest.psicogest.model.enums.AppointmentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RecurringAppointmentCreateDTO(

                @NotNull Long patientId,

                Long clinicMembershipId,

                @NotNull LocalDateTime firstScheduledStart,

                @NotNull LocalDateTime firstScheduledEnd,

                @NotNull AppointmentType appointmentType,

                @Min(value = 2, message = "Recorrência deve possuir pelo menos 2 sessões") @Max(value = 52, message = "Recorrência não pode ultrapassar 52 sessões") int occurrences

) {
}