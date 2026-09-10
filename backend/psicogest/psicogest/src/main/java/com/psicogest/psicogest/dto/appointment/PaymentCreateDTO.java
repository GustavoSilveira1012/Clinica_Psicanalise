package com.psicogest.psicogest.dto.appointment;

import java.math.BigDecimal;

import com.psicogest.psicogest.model.entity.Payment.PaymentMethod;

import jakarta.validation.constraints.*;


public record PaymentCreateDTO(

        @NotNull
        Long patientId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        @NotNull
        PaymentMethod paymentMethod,

        String provider,

        String providerTransactionId

) {
}