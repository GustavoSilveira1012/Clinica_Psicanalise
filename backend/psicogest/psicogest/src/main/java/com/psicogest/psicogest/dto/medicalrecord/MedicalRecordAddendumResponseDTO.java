package com.psicogest.psicogest.dto.medicalrecord;

import java.time.Instant;
import java.util.UUID;

public record MedicalRecordAddendumResponseDTO(

        UUID id,

        UUID medicalRecordId,

        Long authorPsychoanalystId,

        String authorName,

        String reason,

        String content,

        Instant createdAt

) {
}