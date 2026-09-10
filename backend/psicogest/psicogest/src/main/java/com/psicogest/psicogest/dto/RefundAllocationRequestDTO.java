package com.psicogest.psicogest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para alocação de reembolso
 */
public record RefundAllocationRequestDTO(

        /**
         * ID da alocação a reverter
         */
        @NotNull(message = "ID da alocação é obrigatório")
        UUID paymentAllocationId,

        /**
         * Valor a reverter
         */
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(
                value = "0.01",
                message = "Valor deve ser >= 0.01"
        )
        BigDecimal amount

) {
}
