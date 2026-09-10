package com.psicogest.psicogest.model.enums;

/**
 * Direção do movimento de crédito
 */
public enum CreditEntryDirection {
    /**
     * CREDIT: positivo, aumenta saldo
     * Exemplo: cancelamento de cobrança gera crédito
     */
    CREDIT,

    /**
     * DEBIT: negativo, diminui saldo
     * Exemplo: aplicação de crédito em nova cobrança
     */
    DEBIT
}
