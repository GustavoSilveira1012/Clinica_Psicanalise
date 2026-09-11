package com.psicogest.psicogest.dto;

import jakarta.validation.constraints.NotNull;

public record CancelSubscriptionRequest(
        @NotNull String reason,
        boolean atPeriodEnd
) {
}
