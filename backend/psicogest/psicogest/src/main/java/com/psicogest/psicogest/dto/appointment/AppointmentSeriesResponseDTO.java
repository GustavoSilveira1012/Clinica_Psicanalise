package com.psicogest.psicogest.dto.appointment;

import com.psicogest.psicogest.model.enums.*;

import java.time.*;
import java.util.UUID;

public record AppointmentSeriesResponseDTO(

        UUID id,

        UUID previousSeriesId,

        LocalDateTime supersededAt,

        LocalDate supersededFrom,

        Long patientId,
        String patientName,

        Long psychoanalystId,
        String psychoanalystName,

        Long clinicMembershipId,

        RecurrenceFrequency frequency,

        Integer recurrenceInterval,

        DayOfWeek dayOfWeek,

        LocalTime startTime,

        Integer durationMinutes,

        LocalDate startsOn,

        LocalDate endsOn,

        Integer totalOccurrences,

        AppointmentSeriesStatus status

) {
}