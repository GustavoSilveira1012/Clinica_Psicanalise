package com.psicogest.psicogest.model.enums;

/**
 * Modo de liquidação do cancelamento de cobrança
 */
public enum ReceivableCancellationMode {
    /**
     * Sem movimento financeiro
     * Usado quando nada foi pago
     */
    NONE,

    /**
     * Reembolsar o valor pago
     * Inicia refunds para cada payment envolvido
     */
    REFUND,

    /**
     * Converter em saldo credor
     * Valor pago vira crédito disponível
     */
    CREDIT_BALANCE
}
