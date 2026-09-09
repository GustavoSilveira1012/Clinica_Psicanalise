package com.psicogest.psicogest.dto.medicalrecord;

import java.time.Instant;
import java.util.UUID;

/**
 * 21. DTO para leitura de revisão (com conteúdo descriptografado)
 * 
 * Acesso restrito apenas ao autor original (canReadMedicalRecordRevision)
 */
public record MedicalRecordRevisionResponseDTO(

        UUID id,

        UUID medicalRecordId,

        Long revisionNumber,

        Long authorPsychoanalystId,

        String authorName,

        String content,

        Instant createdAt

) {
}
