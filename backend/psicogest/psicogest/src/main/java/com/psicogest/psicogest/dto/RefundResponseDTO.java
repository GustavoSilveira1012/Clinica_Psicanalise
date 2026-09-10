package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.entity.Refund.RefundReason;
import com.psicogest.psicogest.model.entity.Refund.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de resposta para reembolso
 * 
 * 45. Nunca expor:
 * - provider secret
 * - raw credentials
 * - payment token
 */
public record RefundResponseDTO(

        /**
         * ID do reembolso
         */
        UUID id,

        /**
         * ID do pagamento
         */
        UUID paymentId,

        /**
         * Valor reembolsado
         */
        BigDecimal amount,

        /**
         * Moeda
         */
        String currency,

        /**
         * Motivo do reembolso
         */
        RefundReason reason,

        /**
         * Status
         */
        RefundStatus status,

        /**
         * Quando foi solicitado
         */
        Instant requestedAt,

        /**
         * Quando foi confirmado
         */
        Instant confirmedAt,

        /**
         * Quando falhou
         */
        Instant failedAt,

        /**
         * Quando foi cancelado
         */
        Instant cancelledAt

) {
}
