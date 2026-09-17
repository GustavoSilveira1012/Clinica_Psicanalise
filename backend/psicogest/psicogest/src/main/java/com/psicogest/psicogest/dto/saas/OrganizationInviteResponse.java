package com.psicogest.psicogest.dto.saas;

import java.time.Instant;
import java.util.UUID;

public record OrganizationInviteResponse(
        UUID inviteId,
        String maskedEmail,
        String oneTimeToken,
        Instant expiresAt
) {
}
