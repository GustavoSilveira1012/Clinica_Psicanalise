package com.psicogest.psicogest.repository.projection;

import java.time.Instant;
import java.util.UUID;

/**
 * 7. Projection para timeline clínica unificada
 * 
 * Combina eventos de múltiplas entities:
 * - APPOINTMENT_COMPLETED
 * - MEDICAL_RECORD_FINALIZED
 * - MEDICAL_RECORD_ADDENDUM_CREATED
 * - TherapeuticRelationship (futuro)
 * 
 * Apenas metadados (SEM encrypted_content)
 */
public interface ClinicalTimelineProjection {

    /**
     * ID único do evento (UUID convertido para String)
     */
    String getEventId();

    /**
     * Tipo do evento: APPOINTMENT_COMPLETED, MEDICAL_RECORD_FINALIZED, etc
     */
    String getEventType();

    /**
     * Quando o evento ocorreu
     */
    Instant getOccurredAt();

    /**
     * Paciente relacionado ao evento
     */
    Long getPatientId();

    /**
     * Psicanalista envolvido
     */
    Long getPsychoanalystId();

    /**
     * Nome do psicanalista (para exibição)
     */
    String getPsychoanalystName();

    /**
     * ID do prontuário (se aplicável)
     */
    String getMedicalRecordId();

    /**
     * ID da consulta (se aplicável)
     */
    Long getAppointmentId();

    /**
     * ID do addendum (se aplicável)
     */
    String getAddendumId();
}
