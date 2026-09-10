package com.psicogest.psicogest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 19. DTO para criação de receivable a partir de consulta
 * 
 * POST /appointments/{appointmentId}/receivable
 * 
 * Backend deriva:
 * - patientId do Appointment (evita inconsistência)
 * - netAmount = grossAmount - discountAmount
 * 
 * Cliente não envia netAmount (servidor calcula)
 */
public record ReceivableCreateForAppointmentDTO(

        /**
         * Descrição do serviço/consulta
         */
        @NotBlank(message = "Descrição obrigatória")
        String description,

        /**
         * Valor bruto (sem desconto)
         */
        @NotNull(message = "Valor bruto obrigatório")
        @DecimalMin(value = "0.01", message = "Valor bruto deve ser maior que 0")
        BigDecimal grossAmount,

        /**
         * Desconto aplicado (opcional, default 0)
         */
        BigDecimal discountAmount,

        /**
         * Data de vencimento
         */
        @NotNull(message = "Data de vencimento obrigatória")
        LocalDate dueDate

) {
    /**
     * Normaliza valores na construção
     */
    public ReceivableCreateForAppointmentDTO {
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
    }
}
