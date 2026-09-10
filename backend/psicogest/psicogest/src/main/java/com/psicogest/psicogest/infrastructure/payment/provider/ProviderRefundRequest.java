package com.psicogest.psicogest.infrastructure.payment.provider;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 5. Requisição de reembolso para o provider
 */
public record ProviderRefundRequest(

        /**
         * ID do reembolso (nosso domínio)
         */
        UUID refundId,

        /**
         * ID da transação original no provider
         */
        String providerTransactionId,

        /**
         * Valor a reembolsar
         */
        BigDecimal amount,

        /**
         * Moeda
         */
        String currency,

        /**
         * Chave de idempotência
         */
        String idempotencyKey

) {
}
