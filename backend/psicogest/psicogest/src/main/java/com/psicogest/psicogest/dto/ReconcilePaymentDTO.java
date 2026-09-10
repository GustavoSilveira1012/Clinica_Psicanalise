package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request para reconciliação de lançamento bancário com Payment
 * 
 * POST /bank-transactions/{transactionId}/reconcile/payment
 */
public record ReconcilePaymentDTO(

        @NotNull
        UUID paymentId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount
) {
}
