package com.psicogest.psicogest.infrastructure.payment.provider;

import com.psicogest.psicogest.model.entity.Payment.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 3. Requisição genérica de pagamento para o provider
 * 
 * Não inclui entidades completas
 * Apenas dados essenciais para criar a transação
 */
public record ProviderPaymentRequest(

        /**
         * ID do pagamento (nosso domínio)
         */
        UUID paymentId,

        /**
         * Valor
         */
        BigDecimal amount,

        /**
         * Moeda
         */
        String currency,

        /**
         * Método de pagamento
         */
        PaymentMethod paymentMethod,

        /**
         * Descrição para o cliente
         */
        String description,

        /**
         * Referência do cliente (email, CPF, etc)
         * Nunca patient ID inteira
         */
        String customerReference,

        /**
         * Chave de idempotência
         */
        String idempotencyKey

) {
}
