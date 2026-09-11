package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.PackageCancellationReason;
import com.psicogest.psicogest.model.enums.PackageCancellationSettlementMode;
import com.psicogest.psicogest.model.enums.PackageCancellationStatus;

/**
 * Response do cancelamento de pacote
 * 
 * Retornado após a execução bem-sucedida do cancelamento.
 * Contém os valores finais calculados e histórico da operação.
 */
public record PackageCancellationResponseDTO(

        /**
         * ID único do cancelamento
         */
        UUID cancellationId,

        /**
         * ID do pacote cancelado
         */
        UUID patientPackageId,

        /**
         * Motivo do cancelamento
         */
        PackageCancellationReason reason,

        /**
         * Modo de liquidação aplicado
         */
        PackageCancellationSettlementMode settlementMode,

        /**
         * Status atual do cancelamento
         */
        PackageCancellationStatus status,

        /**
         * Snapshot: valor total de compra
         */
        BigDecimal originalPackageAmount,

        /**
         * Snapshot: valor economicamente consumido
         */
        BigDecimal consumedValue,

        /**
         * Snapshot: valor de serviço disponível
         */
        BigDecimal remainingServiceValue,

        /**
         * Snapshot: valor pago antes do cancelamento
         */
        BigDecimal paidAmountBefore,

        /**
         * Snapshot: ajuste aplicado na cobrança
         */
        BigDecimal receivableAdjustmentAmount,

        /**
         * Snapshot: valor a ser liquidado
         */
        BigDecimal settlementAmount,

        /**
         * Snapshot: valor consumido ainda não pago
         */
        BigDecimal outstandingConsumedAmount,

        /**
         * Quando o cancelamento foi solicitado
         */
        Instant requestedAt,

        /**
         * Quem solicitou o cancelamento (ID do usuário)
         */
        Long requestedByUserId

) {}
