package com.psicogest.psicogest.model.enums;

/**
 * Direção do ajuste de cobrança
 * 
 * Define se o ajuste aumenta ou diminui o valor da cobrança
 */
public enum ReceivableAdjustmentDirection {

    /**
     * Aumenta o valor da cobrança
     * Exemplo: multa por atraso, juros
     */
    INCREASE,

    /**
     * Diminui o valor da cobrança
     * Exemplo: desconto, abatimento
     */
    DECREASE
}
