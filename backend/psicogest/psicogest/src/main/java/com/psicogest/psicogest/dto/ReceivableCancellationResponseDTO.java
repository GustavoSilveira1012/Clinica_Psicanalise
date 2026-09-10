package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.enums.ReceivableCancellationMode;
import com.psicogest.psicogest.model.enums.ReceivableCancellationReason;
import com.psicogest.psicogest.model.entity.ReceivableCancellation.ReceivableCancellationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de resposta para cancelamento de cobrança
 */
public record ReceivableCancellationResponseDTO(

        /**
         * ID do cancelamento
         */
        UUID id,

        /**
         * ID da cobrança cancelada
         */
        UUID receivableId,

        /**
         * Motivo
         */
        ReceivableCancellationReason reason,

        /**
         * Modo de liquidação
         */
        ReceivableCancellationMode mode,

        /**
         * Status
         */
        ReceivableCancellationStatus status,

        /**
         * Quando foi solicitado
         */
        Instant createdAt,

        /**
         * Quando foi concluído
         */
        Instant completedAt

) {
}
