package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.entity.Payment.PaymentMethod;
import com.psicogest.psicogest.model.entity.Payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 39. DTO de resposta para pagamento
 * 
 * Inclui saldos calculados
 * 
 * NUNCA expõe:
 * ❌ provider secret
 * ❌ payment provider token
 * ❌ raw webhook
 */
public record PaymentResponseDTO(

        UUID id,

        Long patientId,

        Long clinicId,

        BigDecimal amount,

        /**
         * Valor já alocado
         */
        BigDecimal allocatedAmount,

        /**
         * Saldo disponível para alocar
         */
        BigDecimal availableAmount,

        String currency,

        PaymentMethod paymentMethod,

        PaymentStatus status,

        Instant receivedAt,

        Instant createdAt

) {
}
