package com.psicogest.psicogest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para aplicação de crédito em cobrança
 */
public record ApplyCreditDTO(

        /**
         * Valor de crédito a aplicar
         */
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(
                value = "0.01",
                message = "Valor deve ser >= 0.01"
        )
        BigDecimal amount

) {
}
