package com.psicogest.psicogest.dto.relationship;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TherapeuticRelationshipCreateDTO(

                @NotNull(message = "Paciente é obrigatório") Long patientId,

                Boolean primary,

                /*
                 * Pode ser usado ao migrar registros antigos.
                 * Se null, usamos agora.
                 */
                LocalDateTime startedAt

) {
}