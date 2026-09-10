package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.entity.Refund.RefundReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para criação de reembolso
 * 
 * POST /payments/{paymentId}/refunds
 */
public record RefundCreateDTO(

        /**
         * Valor a reembolsar
         */
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(
                value = "0.01",
                message = "Valor deve ser >= 0.01"
        )
        BigDecimal amount,

        /**
         * Motivo do reembolso
         */
        @NotNull(message = "Motivo é obrigatório")
        RefundReason reason,

        /**
         * Alocações a reverter
         */
        @NotEmpty(message = "Deve haver pelo menos uma alocação a reverter")
        @Valid
        List<RefundAllocationRequestDTO> allocations

) {
}
