package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.entity.Payment.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para criação de pagamento
 * 
 * POST /payments
 * Header: Idempotency-Key
 * 
 * 12. Fingerprint: patientId | amount | paymentMethod | provider | providerTransactionId
 */
public record PaymentCreateDTO(

        /**
         * ID do paciente (obrigatório para fingerprint)
         */
        @NotNull(message = "ID do paciente obrigatório")
        Long patientId,

        /**
         * Valor do pagamento
         */
        @NotNull(message = "Valor obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que 0")
        BigDecimal amount,

        /**
         * Forma de pagamento
         */
        @NotNull(message = "Forma de pagamento obrigatória")
        PaymentMethod paymentMethod,

        /**
         * Gateway/Provider (Stripe, MercadoPago, etc) - opcional, usado no fingerprint
         */
        String provider,

        /**
         * ID da transação no provider - opcional, usado no fingerprint
         */
        String providerTransactionId,

        /**
         * Descrição/referência do pagamento (opcional)
         */
        String description

) {
}
