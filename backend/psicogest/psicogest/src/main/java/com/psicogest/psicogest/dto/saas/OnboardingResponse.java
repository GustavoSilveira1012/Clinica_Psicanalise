package com.psicogest.psicogest.dto.saas;

import com.psicogest.psicogest.model.enums.OnboardingStatus;
import com.psicogest.psicogest.model.enums.OnboardingStep;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OnboardingResponse(
        UUID organizationId,
        OnboardingStep currentStep,
        OnboardingStatus status,
        List<String> completedSteps,
        Instant completedAt
) {
}
