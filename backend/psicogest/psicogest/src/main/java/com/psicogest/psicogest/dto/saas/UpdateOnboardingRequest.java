package com.psicogest.psicogest.dto.saas;

import com.psicogest.psicogest.model.enums.OnboardingStep;
import jakarta.validation.constraints.NotNull;

public record UpdateOnboardingRequest(@NotNull OnboardingStep step) {
}
