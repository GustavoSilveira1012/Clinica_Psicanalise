package com.psicogest.psicogest.dto.medicalrecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MedicalRecordUpdateDTO(

        @NotBlank( message = "Conteúdo clínico não pode estar vazio" )
        @Size( min = 1, max = 100000, message = "Conteúdo deve ter entre 1 e 100000 caracteres" )
        String content
) {
}
