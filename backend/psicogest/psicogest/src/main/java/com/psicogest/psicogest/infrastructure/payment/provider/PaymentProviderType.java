package com.psicogest.psicogest.infrastructure.payment.provider;

/**
 * Tipos de gateway de pagamento suportados
 */
public enum PaymentProviderType {
    /**
     * Stripe: cartão, PIX, etc
     */
    STRIPE,

    /**
     * MercadoPago: cartão, transferência, etc
     */
    MERCADO_PAGO,

    /**
     * PayPal: cartão, paypal account, etc
     */
    PAYPAL
}
