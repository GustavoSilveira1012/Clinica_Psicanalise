package com.psicogest.psicogest.model.enums;

/**
 * Status de reconciliação de lançamento bancário
 * 
 * Alias para BankReconciliationStatus (mantém compatibilidade)
 */
public enum ReconciliationStatus {

    /**
     * Não conciliado
     */
    UNRECONCILED,

    /**
     * Parcialmente conciliado
     */
    PARTIALLY_RECONCILED,

    /**
     * Totalmente conciliado
     */
    RECONCILED,

    /**
     * Marcado para ignorar
     */
    IGNORED,

    /**
     * Questionado (divergência encontrada)
     */
    DISPUTED
}
