package com.psicogest.psicogest.infrastructure.payment.provider;

/**
 * 5. Resultado de um reembolso no provider
 */
public record ProviderRefundResult(

        /**
         * ID do reembolso no provider
         */
        String providerRefundId,

        /**
         * Status (PENDING, CONFIRMED, FAILED)
         */
        ProviderRefundStatus status

) {
}
