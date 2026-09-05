package com.psicogest.psicogest.dto.availability;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityResponseDTO(

        Long id,

        Long psychoanalystId,

        DayOfWeek dayOfWeek,

        LocalTime startTime,

        LocalTime endTime,

        Boolean active
) {
}