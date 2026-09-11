package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.PackageCancellationPricingPolicy;

/**
 * Preview do cancelamento de pacote
 * 
 * Simula os valores que serão liquidados no cancelamento,
 * sem fazer nenhuma alteração no banco de dados.
 * 
 * É apenas informativo para o usuário confirmar a operação.
 * O backend recalcula tudo novamente durante a execução real
 * com locks pessimistas.
 */
public record PackageCancellationPreviewDTO(

        /**
         * ID do pacote a cancelar
         */
        UUID patientPackageId,

        /**
         * Valor original de compra do pacote
         */
        BigDecimal originalPackageAmount,

        /**
         * Valor economicamente consumido
         * 
         * Calculado por: SUM(cumulativeValue por item com consumos ACTIVE)
         * Não usa granted - remaining (evita impacto de expiração, etc)
         */
        BigDecimal consumedValue,

        /**
         * Valor de serviço ainda disponível
         * 
         * = originalPackageAmount - consumedValue
         */
        BigDecimal remainingServiceValue,

        /**
         * Valor já pago (allocações efetivas)
         */
        BigDecimal paidAmount,

        /**
         * Valor estimado a ser liquidado
         * 
         * Depende da política de pricing:
         * - PRO_RATA: remainingServiceValue (automático)
         * - MANUAL_REVIEW: pode variar conforme revisão
         */
        BigDecimal estimatedSettlementAmount,

        /**
         * Valor ainda pendente de cobrança
         * 
         * = originalPackageAmount - paidAmount
         */
        BigDecimal remainingOutstandingAmount,

        /**
         * Se há valor disponível para refund
         * 
         * true se estimatedSettlementAmount > 0 e há crédito a devolver
         */
        boolean refundAvailable,

        /**
         * Se há valor disponível para conversão em crédito
         * 
         * true se estimatedSettlementAmount > 0 e policy permite
         */
        boolean creditBalanceAvailable,

        /**
         * Política de pricing aplicada
         * 
         * PRO_RATA: cálculo automático
         * MANUAL_REVIEW: requer revisão gerencial
         */
        PackageCancellationPricingPolicy pricingPolicy

) {}
