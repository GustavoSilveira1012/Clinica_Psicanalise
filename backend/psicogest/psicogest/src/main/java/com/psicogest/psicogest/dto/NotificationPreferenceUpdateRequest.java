package com.psicogest.psicogest.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceUpdateRequest(
        @NotNull Boolean enabled
) {
}
