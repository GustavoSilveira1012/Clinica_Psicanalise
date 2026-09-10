package com.psicogest.psicogest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request para ignorar um lançamento bancário
 * 
 * POST /bank-transactions/{id}/ignore
 * 
 * Motivos: tarifa, transferência interna, aporte, juros, etc.
 */
public record IgnoreBankTransactionDTO(

        @NotBlank
        @Size(max = 255)
        String reason
) {
}
