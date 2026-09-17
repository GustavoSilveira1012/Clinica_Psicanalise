package com.psicogest.psicogest.dto.saas;

import com.psicogest.psicogest.model.enums.OrganizationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrganizationInviteRequest(
        @NotBlank @Email String email,
        @NotNull OrganizationRole role
) {
}
