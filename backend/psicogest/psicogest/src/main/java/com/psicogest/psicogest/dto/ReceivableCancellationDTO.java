package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.enums.ReceivableCancellationMode;
import com.psicogest.psicogest.model.enums.ReceivableCancellationReason;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para cancelamento de cobrança
 */
public record ReceivableCancellationDTO(

        /**
         * Motivo do cancelamento
         */
        @NotNull(message = "Motivo é obrigatório")
        ReceivableCancellationReason reason,

        /**
         * Modo de liquidação (opcional se nada foi pago)
         * NONE: sem movimento
         * REFUND: reembolsar
         * CREDIT_BALANCE: converter em crédito
         */
        ReceivableCancellationMode mode

) {
}
