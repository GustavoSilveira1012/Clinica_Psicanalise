package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.entity.Payment.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/** Request for recording, confirming, and allocating one payment atomically. */
public record PaymentCollectionCreateDTO(
        @NotNull(message = "ID do paciente obrigatório") Long patientId,
        @NotNull(message = "ID da cobrança obrigatório") UUID receivableId,
        @NotNull(message = "Valor obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero") BigDecimal amount,
        @NotNull(message = "Forma de pagamento obrigatória") PaymentMethod paymentMethod,
        String description
) {
}
