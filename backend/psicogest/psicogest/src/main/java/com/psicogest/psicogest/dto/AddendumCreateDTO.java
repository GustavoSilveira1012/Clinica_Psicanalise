package com.psicogest.psicogest.dto;

import com.psicogest.psicogest.model.enums.MedicalRecordAddendumReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para criar novo addendum em prontuário finalizado
 * 
 * reason: não-sensível (CLARIFICATION, CORRECTION, COMPLEMENT, OTHER)
 * content: sensível (cifrado no banco com AES-256-GCM)
 */
public record AddendumCreateDTO(

        @NotBlank
        @Size(max = 100_000)
        String content,

        @NotNull
        MedicalRecordAddendumReason reason

) {
}
