package com.psicogest.psicogest.infrastructure.payment.provider;

/**
 * Status do pagamento reportado pelo provider
 * 
 * Separado do nosso PaymentStatus para não contaminar o domínio
 * 
 * 4. Resultado genérico
 */
public enum ProviderPaymentStatus {
    /**
     * Aguardando confirmação (ex: PIX pendente)
     */
    PENDING,

    /**
     * Confirmado/capturado
     */
    CONFIRMED,

    /**
     * Falhou
     */
    FAILED
}
