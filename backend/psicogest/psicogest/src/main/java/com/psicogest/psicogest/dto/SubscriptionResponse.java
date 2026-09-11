package com.psicogest.psicogest.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.SubscriptionStatus;

public record SubscriptionResponse(
        UUID id,
        Long patientId,
        UUID subscriptionPlanVersionId,
        SubscriptionStatus status,
        LocalDate startsOn,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        LocalDate nextCycleStart,
        Integer billingAnchorDay,
        boolean cancelAtPeriodEnd,
        Instant createdAt
) {
}
