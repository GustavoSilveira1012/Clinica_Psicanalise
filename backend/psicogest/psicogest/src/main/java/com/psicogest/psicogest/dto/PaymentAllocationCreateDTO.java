package com.psicogest.psicogest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 19. DTO para alocação de pagamento em cobrança
 * 
 * POST /payments/{paymentId}/allocations
 */
public record PaymentAllocationCreateDTO(

        /**
         * ID da cobrança a receber
         */
        @NotNull(message = "ID da cobrança obrigatório")
        UUID receivableId,

        /**
         * Valor a alocar
         */
        @NotNull(message = "Valor obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que 0")
        BigDecimal amount

) {
}
