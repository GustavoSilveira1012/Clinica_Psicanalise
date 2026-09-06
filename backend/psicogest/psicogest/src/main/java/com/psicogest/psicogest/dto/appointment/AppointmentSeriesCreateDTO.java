package com.psicogest.psicogest.dto.appointment;

import com.psicogest.psicogest.model.enums.AppointmentType;
import com.psicogest.psicogest.model.enums.RecurrenceFrequency;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentSeriesCreateDTO(

                @NotNull Long patientId,

                Long clinicMembershipId,

                @NotNull RecurrenceFrequency frequency,

                @Min(1) @Max(12) Integer recurrenceInterval,

                @NotNull LocalDate startsOn,

                LocalDate endsOn,

                @Min(2) @Max(104) Integer totalOccurrences,

                @NotNull LocalTime startTime,

                @Min(10) @Max(480) Integer durationMinutes,

                @NotNull AppointmentType appointmentType

) {
}