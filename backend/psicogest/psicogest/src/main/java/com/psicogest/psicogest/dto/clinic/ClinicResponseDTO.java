package com.psicogest.psicogest.dto.clinic;

public record ClinicResponseDTO(
    Long id,
    String name,
    String cnpj,
    Boolean active
) {
}
