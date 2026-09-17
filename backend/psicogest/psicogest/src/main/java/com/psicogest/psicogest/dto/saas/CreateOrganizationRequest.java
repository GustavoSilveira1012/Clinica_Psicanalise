package com.psicogest.psicogest.dto.saas;

import com.psicogest.psicogest.model.enums.OrganizationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(min = 4, max = 80)
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,78}[a-z0-9]$", message = "slug inválido")
        String slug,
        @NotNull OrganizationType type,
        @Size(max = 80) String timezone
) {
}
