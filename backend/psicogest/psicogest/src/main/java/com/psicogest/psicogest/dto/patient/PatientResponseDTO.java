package com.psicogest.psicogest.dto.patient;

import java.time.LocalDate;

public record PatientResponseDTO(

        Long id,
        String name,
        String email,
        String phone,
        LocalDate birthDate,
        Boolean active
) {
    
}
