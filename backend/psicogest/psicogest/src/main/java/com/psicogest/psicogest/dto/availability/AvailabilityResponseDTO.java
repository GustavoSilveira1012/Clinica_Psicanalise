package com.psicogest.psicogest.dto.availability;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.transaction.Transactional;
@Transactional
public record AvailabilityResponseDTO(

        Long id,

        Long psychoanalystId,

        DayOfWeek dayOfWeek,

        LocalTime startTime,

        LocalTime endTime,

        Boolean active
) {
}