package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de resposta para alocação de pagamento
 */
public record PaymentAllocationResponseDTO(

        UUID id,

        UUID paymentId,

        UUID receivableId,

        BigDecimal amount,

        Instant createdAt

) {
}
