package com.psicogest.psicogest.dto;

import java.math.BigDecimal;

/**
 * Cálculo interno do cancelamento de pacote
 * 
 * Armazena os valores calculados durante o cancelamento.
 * Não é enviado ao frontend, é apenas para uso interno do backend.
 * 
 * Fórmulas aplicadas:
 * - adjustmentAmount = remainingServiceValue (ajuste na cobrança)
 * - adjustedReceivable = currentEffective - adjustment
 * - settlement = effectivePaid - adjustedReceivable (valor a devolver)
 * - outstanding = adjustedReceivable - effectivePaid (valor ainda não pago)
 */
public record PackageCancellationCalculation(

        /**
         * Valor total de compra do pacote
         */
        BigDecimal packageAmount,

        /**
         * Valor economicamente consumido
         * 
         * Calculado por: SUM(cumulativeValue por item com consumos ACTIVE)
         */
        BigDecimal consumedValue,

        /**
         * Valor de serviço ainda disponível
         * 
         * = packageAmount - consumedValue
         */
        BigDecimal remainingServiceValue,

        /**
         * Valor efetivamente pago antes do cancelamento
         * 
         * Alocações efetivas (considerando refunds)
         */
        BigDecimal paidBefore,

        /**
         * Valor do ajuste na cobrança
         * 
         * = remainingServiceValue
         * Será criado como ReceivableAdjustment DECREASE
         */
        BigDecimal adjustmentAmount,

        /**
         * Valor a ser liquidado (refund ou crédito)
         * 
         * = effectivePaid - adjustedReceivable
         * onde adjustedReceivable = effectiveReceivable - adjustmentAmount
         */
        BigDecimal settlementAmount,

        /**
         * Valor consumido ainda não pago
         * 
         * = adjustedReceivable - effectivePaid
         * Permanece pendente de cobrança
         */
        BigDecimal outstandingConsumedAmount

) {}
