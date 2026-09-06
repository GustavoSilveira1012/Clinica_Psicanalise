package com.psicogest.psicogest.dto.relationship;

import com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus;

import java.time.LocalDateTime;

public record TherapeuticRelationshipResponseDTO(

                Long id,

                Long patientId,

                String patientName,

                Long psychoanalystId,

                String psychoanalystName,

                TherapeuticRelationshipStatus status,

                Boolean primary,

                LocalDateTime startedAt,

                LocalDateTime endedAt,

                String endReason

) {
}