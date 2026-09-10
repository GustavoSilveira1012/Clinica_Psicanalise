package com.psicogest.psicogest.model.enums;

/**
 * Status do repasse recebido do gateway
 * 
 * RECEIVED: informações básicas recebidas, aguardando validação
 * VALIDATED: saldo validado e bateu com o informado
 * SETTLED: gateway confirmou que pagou para a conta
 * MISMATCHED: divergência entre o informado e o calculado
 */
public enum ProviderSettlementStatus {

    /**
     * Repasse recebido do gateway, aguardando validação
     */
    RECEIVED,

    /**
     * Saldo validado e bateu
     */
    VALIDATED,

    /**
     * Gateway confirmou o pagamento para conta bancária
     */
    SETTLED,

    /**
     * Divergência encontrada durante validação
     */
    MISMATCHED
}
