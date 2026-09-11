package com.psicogest.psicogest.model.enums;

/**
 * Modo de liquidação de cancelamento de pacote
 * 
 * Define como o valor será tratado quando um pacote é cancelado
 */
public enum PackageCancellationSettlementMode {

    /**
     * Sem liquidação
     * 
     * O valor não é devolvido nem convertido em crédito.
     * Usado quando há refusão de política ou motivos comerciais.
     */
    NONE,

    /**
     * Devolução de valor
     * 
     * Cria um refund que é revertido para o payment original
     * ou dinheiro em conta (dependendo do provedor).
     */
    REFUND,

    /**
     * Conversão em crédito
     * 
     * Cria um crédito (SessionCreditEntry) que pode ser
     * usado em futuras compras de pacotes ou sessões.
     */
    CREDIT_BALANCE
}
