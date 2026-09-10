package com.psicogest.psicogest.model.enums;

/**
 * Tipo de item em um repasse
 * 
 * PAYMENT: pagamento recebido do cliente
 * REFUND: reembolso processado
 * PROVIDER_FEE: taxa cobrada pelo gateway
 * CHARGEBACK: disputa/chargeback
 * ADJUSTMENT: ajuste ou correção
 */
public enum ProviderSettlementItemType {

    /**
     * Pagamento recebido do cliente
     */
    PAYMENT,

    /**
     * Reembolso processado
     */
    REFUND,

    /**
     * Taxa cobrada pelo gateway
     */
    PROVIDER_FEE,

    /**
     * Chargeback ou disputa
     */
    CHARGEBACK,

    /**
     * Ajuste ou correção
     */
    ADJUSTMENT
}
