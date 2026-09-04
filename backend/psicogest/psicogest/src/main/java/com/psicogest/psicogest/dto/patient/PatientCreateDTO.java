package com.psicogest.psicogest.dto.patient;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record PatientCreateDTO(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150)
        String name,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String password,

        @Size(max = 30)
        String phone,

        @Past(message = "Data de nascimento deve ser anterior à data atual")
        LocalDate birthDate

) {

    public String getEmail() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEmail'");
    }
    
}
