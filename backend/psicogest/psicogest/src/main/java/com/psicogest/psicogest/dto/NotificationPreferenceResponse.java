package com.psicogest.psicogest.dto;

import java.util.UUID;

public record NotificationPreferenceResponse(
        UUID id,
        String notificationType,
        String channel,
        String label,
        String description,
        boolean enabled,
        boolean required
) {
}
