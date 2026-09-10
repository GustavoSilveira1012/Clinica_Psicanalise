package com.psicogest.psicogest.infrastructure.payment.provider.webhook;

/**
 * 10. Tipos de eventos de webhook normalizados
 * 
 * Cada provider tem seus próprios nomes de evento.
 * Normalizamos para esses tipos.
 */
public enum PaymentProviderEventType {
    /**
     * Pagamento foi capturado/confirmado
     */
    PAYMENT_CONFIRMED,

    /**
     * Pagamento falhou
     */
    PAYMENT_FAILED,

    /**
     * Pagamento foi cancelado
     */
    PAYMENT_CANCELLED,

    /**
     * Reembolso foi processado
     */
    REFUND_CONFIRMED,

    /**
     * Reembolso falhou
     */
    REFUND_FAILED
}

