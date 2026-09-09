package com.psicogest.psicogest.dto.medicalrecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MedicalRecordUpdateDTO(

        @NotBlank
        @Size(max = 100_000)
        String content

) {
}