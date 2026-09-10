package com.psicogest.psicogest.model.enums;

/**
 * Status de validação do repasse
 * 
 * Distinct do ProviderSettlementStatus:
 * - Status = ciclo de vida
 * - ValidationStatus = resultado da validação
 */
public enum ProviderSettlementValidationStatus {

    /**
     * Ainda não foi validado
     */
    PENDING,

    /**
     * Saldo calculado bate com o informado
     */
    MATCHED,

    /**
     * Divergência encontrada (direção ou valor)
     */
    MISMATCHED,

    /**
     * Validação foi refeita e agora bate
     */
    REMATCH
}
