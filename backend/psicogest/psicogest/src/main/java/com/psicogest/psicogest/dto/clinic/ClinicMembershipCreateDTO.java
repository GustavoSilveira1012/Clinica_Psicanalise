package com.psicogest.psicogest.dto.clinic;

import jakarta.validation.constraints.NotNull;

public record ClinicMembershipCreateDTO(
    @NotNull(message = "O ID do pscanalista é obrigatório")
    Long psychoanalystId
) {   
}