package com.psicogest.psicogest.dto.medicalrecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MedicalRecordCreateDTO(

        Long appointmentId,

        @NotBlank(
                message = "Conteúdo clínico é obrigatório"
        )

        @Size(
                max = 100_000,
                message = "Conteúdo clínico excede o limite permitido"
        )

        String content

) {
}