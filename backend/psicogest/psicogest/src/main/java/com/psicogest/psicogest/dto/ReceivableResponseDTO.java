package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.entity.Receivable.ReceivableStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 37. DTO de resposta para cobrança
 * 
 * GET /receivables/{receivableId}
 * 
 * Inclui saldos calculados (paidAmount, outstandingAmount)
 * e status temporal (overdue)
 */
public record ReceivableResponseDTO(

        UUID id,

        Long patientId,

        Long clinicId,

        Long appointmentId,

        String description,

        BigDecimal grossAmount,

        BigDecimal discountAmount,

        BigDecimal netAmount,

        /**
         * Valor já alocado (confirmado + partially_refunded)
         */
        BigDecimal paidAmount,

        /**
         * Saldo pendente (netAmount - paidAmount)
         */
        BigDecimal outstandingAmount,

        /**
         * true se vencido E com saldo pendente E não cancelada
         */
        boolean overdue,

        ReceivableStatus status,

        LocalDate dueDate,

        Instant createdAt

) {
}
