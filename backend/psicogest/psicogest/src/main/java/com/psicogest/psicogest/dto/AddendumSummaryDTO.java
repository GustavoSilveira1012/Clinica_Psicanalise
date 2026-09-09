package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.enums.MedicalRecordAddendumReason;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO para listagem de addendums (metadados apenas, SEM content)
 * 
 * Usado em timeline do prontuário para evitar descriptografar múltiplos addendums
 * Para ler o conteúdo: GET /medical-record-addendums/{id}
 */
public record AddendumSummaryDTO(

        UUID id,

        String authorName,

        MedicalRecordAddendumReason reason,

        Instant createdAt

) {
}
