package com.psicogest.psicogest.dto.appointment;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;


public record ReceivableCreateDTO(

        @NotNull
        Long patientId,

        Long appointmentId,

        @NotBlank
        @Size(max = 255)
        String description,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal grossAmount,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal discountAmount,

        @NotNull
        LocalDate dueDate

) {
}