package com.psicogest.psicogest.infrastructure.payment.provider;

/**
 * Status do reembolso reportado pelo provider
 * 
 * Separado do nosso RefundStatus para não contaminar o domínio
 */
public enum ProviderRefundStatus {
    /**
     * Aguardando processamento
     */
    PENDING,

    /**
     * Confirmado/processado
     */
    CONFIRMED,

    /**
     * Falhou
     */
    FAILED
}
