package com.psicogest.psicogest.dto.availability;

import com.psicogest.psicogest.model.enums.AvailabilityExceptionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityExceptionUpdateDTO(

        @NotNull(message = "Data é obrigatória")
        LocalDate date,

        @NotNull(message = "Tipo da exceção é obrigatório")
        AvailabilityExceptionType type,

        LocalTime startTime,

        LocalTime endTime,

        @Size(
                max = 255,
                message = "Motivo deve possuir no máximo 255 caracteres"
        )
        String reason

) {
}
