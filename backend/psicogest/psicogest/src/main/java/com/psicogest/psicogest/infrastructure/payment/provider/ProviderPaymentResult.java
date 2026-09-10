package com.psicogest.psicogest.infrastructure.payment.provider;

import java.time.Instant;

/**
 * 4. Resultado da criação de pagamento no provider
 * 
 * Contém dados essenciais do provider
 * Status separado (ProviderPaymentStatus) - nunca PaymentStatus
 */
public record ProviderPaymentResult(

        /**
         * ID da transação no provider
         */
        String providerTransactionId,

        /**
         * Status do provider (PENDING, CONFIRMED, FAILED)
         */
        ProviderPaymentStatus status,

        /**
         * URL de pagamento (para cartão, PIX, etc)
         */
        String paymentUrl,

        /**
         * Cópia PIX (para PIX QR Code)
         */
        String pixCopyPaste,

        /**
         * Quando expira (para PIX e outros)
         */
        Instant expiresAt

) {
}
