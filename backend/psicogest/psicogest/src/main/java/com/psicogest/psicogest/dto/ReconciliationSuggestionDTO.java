package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.PaymentMethod;
import com.psicogest.psicogest.model.enums.ReconciliationConfidence;

/**
 * Sugestão de reconciliação de lançamento bancário
 * 
 * Contém:
 * - Payment candidato
 * - Valor e datas
 * - Nível de confiança
 * - Motivo (qual critério disparou a sugestão)
 */
public record ReconciliationSuggestionDTO(

        UUID paymentId,

        BigDecimal amount,

        LocalDate paymentDate,

        PaymentMethod paymentMethod,

        ReconciliationConfidence confidence,

        String reason
) {
}
