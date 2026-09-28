package com.psicogest.psicogest.dto.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientUpdateDTO(
        @Size(max = 150) String name,
        @Email(message = "E-mail inválido") String email,
        @Size(max = 30) String phone,
        @Past(message = "Data de nascimento deve ser anterior à data atual") LocalDate birthDate
) {
}
