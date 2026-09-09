package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.enums.MedicalRecordStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO para listagem de prontuários (metadados apenas)
 * Sem content para evitar descriptografar 50 prontuários sem necessidade
 * Permite exfiltration detection mais preciso
 */
public record MedicalRecordSummaryDTO(

        UUID id,

        Long authorPsychoanalystId,

        String authorName,

        MedicalRecordStatus status,

        Instant createdAt,

        Instant finalizedAt

) {
}
