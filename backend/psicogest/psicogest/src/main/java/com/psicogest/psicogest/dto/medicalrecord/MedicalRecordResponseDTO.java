package com.psicogest.psicogest.dto.medicalrecord;

import com.psicogest.psicogest.model.enums.MedicalRecordStatus;

import java.time.Instant;
import java.util.UUID;

public record MedicalRecordResponseDTO(

        UUID id,

        Long patientId,

        Long authorPsychoanalystId,

        String authorName,

        Long appointmentId,

        MedicalRecordStatus status,

        String content,

        Long version,

        Instant createdAt,

        Instant updatedAt,

        Instant finalizedAt

) {
}