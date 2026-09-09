package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.enums.MedicalRecordAddendumReason;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO para resposta de addendum (com conteúdo descriptografado)
 */
public record AddendumResponseDTO(

        UUID id,

        UUID medicalRecordId,

        Long authorPsychoanalystId,

        String authorName,

        MedicalRecordAddendumReason reason,

        String content,

        Instant createdAt

) {
}
