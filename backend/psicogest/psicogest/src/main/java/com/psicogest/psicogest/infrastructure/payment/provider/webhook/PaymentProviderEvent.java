package com.psicogest.psicogest.infrastructure.payment.provider.webhook;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 10. Evento normalizado de webhook
 * 
 * Assim o processor não precisa saber que um gateway chama:
 *   PAYMENT_RECEIVED
 * e outro:
 *   payment.approved
 * 
 * Os adapters transformam tudo no nosso vocabulário.
 */
public record PaymentProviderEvent(

        /**
         * Tipo de evento normalizado
         */
        PaymentProviderEventType type,

        /**
         * ID da transação no provider
         */
        String providerTransactionId,

        /**
         * ID do reembolso no provider (se aplicável)
         */
        String providerRefundId,

        /**
         * Valor
         */
        BigDecimal amount,

        /**
         * Quando ocorreu
         */
        Instant occurredAt

) {
}
