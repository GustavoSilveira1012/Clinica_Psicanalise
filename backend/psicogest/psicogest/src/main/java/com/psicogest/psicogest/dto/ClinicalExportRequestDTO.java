package com.psicogest.psicogest.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 13. Request para exportação clínica
 * 
 * Exportação é altamente restritiva:
 * - Um paciente por vez (sem patientIds[])
 * - Intervalo de datas explícito
 * - Tipos de documento selecionáveis
 * 
 * Futuras exportações administrativas em massa
 * serão fluxo separado com aprovação adicional.
 */
public record ClinicalExportRequestDTO(

        /**
         * Paciente a exportar
         * Um paciente por vez, nunca múltiplos.
         */
        @NotNull(message = "patientId é obrigatório")
        Long patientId,

        /**
         * Data inicial (inclusive)
         * Opcional: null = desde o início
         */
        LocalDate from,

        /**
         * Data final (inclusive)
         * Opcional: null = até o fim
         */
        LocalDate to,

        /**
         * Incluir prontuários finalizados
         */
        boolean includeMedicalRecords,

        /**
         * Incluir addendums
         */
        boolean includeAddendums,

        /**
         * Incluir consultas completadas
         */
        boolean includeAppointments

) {
}
