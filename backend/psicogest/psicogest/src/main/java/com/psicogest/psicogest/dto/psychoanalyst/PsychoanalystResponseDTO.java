package com.psicogest.psicogest.dto.psychoanalyst;

public record PsychoanalystResponseDTO(

                Long id,

                String name,

                String email,

                String licenseNumber,

                String specialization,

                String bio,

                String phone,

                Boolean active) {
}
