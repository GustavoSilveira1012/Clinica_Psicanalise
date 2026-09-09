package com.psicogest.psicogest.dto.medicalrecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MedicalRecordAddendumCreateDTO(

        @NotBlank(
                message = "O complemento clínico é obrigatório"
        )
        @Size(
                max = 100_000,
                message = "O complemento clínico excede o limite permitido"
        )
        String content,

        @NotBlank(
                message = "O motivo do complemento é obrigatório"
        )
        @Size(
                max = 500,
                message = "O motivo excede o limite permitido"
        )
        String reason

) {
}