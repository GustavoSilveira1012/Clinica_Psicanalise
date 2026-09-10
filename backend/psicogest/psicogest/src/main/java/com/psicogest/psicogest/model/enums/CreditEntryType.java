package com.psicogest.psicogest.model.enums;

/**
 * Tipo de movimento de crédito
 */
public enum CreditEntryType {
    /**
     * Crédito gerado por cancelamento de cobrança
     */
    RECEIVABLE_CANCELLATION,

    /**
     * Crédito aplicado em uma cobrança para reduzir o saldo devido
     */
    RECEIVABLE_APPLICATION
}
