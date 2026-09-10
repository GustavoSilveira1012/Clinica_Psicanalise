package com.psicogest.psicogest.model.enums;

/**
 * Direção do repasse
 * 
 * CREDIT_TO_FINANCIAL_ENTITY: gateway paga para nós (normal)
 * DEBIT_FROM_FINANCIAL_ENTITY: gateway cobra de nós (chargeback, disputa)
 */
public enum ProviderSettlementDirection {

    /**
     * Gateway paga para a entidade (repasse normal)
     */
    CREDIT_TO_FINANCIAL_ENTITY,

    /**
     * Gateway cobra da entidade (chargeback, disputa)
     */
    DEBIT_FROM_FINANCIAL_ENTITY
}
