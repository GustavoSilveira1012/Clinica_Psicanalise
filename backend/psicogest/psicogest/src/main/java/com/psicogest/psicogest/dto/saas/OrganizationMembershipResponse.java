package com.psicogest.psicogest.dto.saas;

import com.psicogest.psicogest.model.enums.OrganizationMembershipStatus;
import com.psicogest.psicogest.model.enums.OrganizationRole;

import java.util.UUID;

public record OrganizationMembershipResponse(
        UUID membershipId,
        UUID organizationId,
        Long userId,
        String userName,
        OrganizationRole role,
        OrganizationMembershipStatus status
) {
}
