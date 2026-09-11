package com.psicogest.psicogest.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSubscriptionPlanRequest(
        @NotNull UUID financialEntityId,
        @NotBlank String name,
        String description
) {
}
