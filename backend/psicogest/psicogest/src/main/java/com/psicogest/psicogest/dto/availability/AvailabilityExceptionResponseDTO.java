package com.psicogest.psicogest.dto.availability;
import com.psicogest.psicogest.model.enums.AvailabilityExceptionType;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityExceptionResponseDTO(

        Long id,

        Long psychoanalystId,

        LocalDate date,

        AvailabilityExceptionType type,

        LocalTime startTime,

        LocalTime endTime,

        Boolean fullDay,

        String reason

) {
}