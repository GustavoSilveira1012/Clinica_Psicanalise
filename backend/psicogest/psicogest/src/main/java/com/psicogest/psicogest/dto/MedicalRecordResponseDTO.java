package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.enums.MedicalRecordStatus;

import java.time.Instant;
import java.util.UUID;

public record MedicalRecordResponseDTO(

        UUID id,

        Long patientId,

        Long psychoanalystId,

        Long therapeuticRelationshipId,

        Long appointmentId,

        MedicalRecordStatus status,

        String content,

        Integer cryptoVersion,

        String cryptoAlgorithm,

        String keyId,

        Long version,

        Instant createdAt,

        Instant updatedAt,

        Instant finalizedAt
) {
}
