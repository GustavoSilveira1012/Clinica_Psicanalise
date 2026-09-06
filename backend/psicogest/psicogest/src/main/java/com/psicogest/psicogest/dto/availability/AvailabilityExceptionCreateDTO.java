package com.psicogest.psicogest.dto.availability;

import com.psicogest.psicogest.model.enums.AvailabilityExceptionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityExceptionCreateDTO(
    @NotNull(message = "A data é obrigatória!")
    LocalDate date,

    @NotNull(message = "O tipo de exceção é obrigatório!")
    AvailabilityExceptionType type,

    LocalTime startTime,

    LocalTime endTime,

    @Size(max = 255, message = "A observação deve possuir no máximo de 255 caracteres!")
    String observation
) {

    public String reason() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reason'");
    }
    
}
