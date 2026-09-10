package com.psicogest.psicogest.model.enums;

/**
 * Direção de um item dentro de um repasse
 * 
 * CREDIT: aumenta o saldo (pagamentos, ajustes positivos)
 * DEBIT: diminui o saldo (refunds, taxas, chargebacks)
 */
public enum SettlementEntryDirection {

    /**
     * Aumenta o saldo do repasse (crédito)
     */
    CREDIT,

    /**
     * Diminui o saldo do repasse (débito)
     */
    DEBIT
}
