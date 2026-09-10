package com.psicogest.psicogest.model.vo;

import java.math.BigDecimal;

import com.psicogest.psicogest.model.enums.ProviderSettlementDirection;

/**
 * Resultado calculado do saldo líquido de um repasse
 * 
 * Contém:
 * - direção (CREDIT_TO_FINANCIAL_ENTITY ou DEBIT_FROM_FINANCIAL_ENTITY)
 * - valor absoluto
 */
public record SettlementNet(

        ProviderSettlementDirection direction,

        BigDecimal amount
) {
}
