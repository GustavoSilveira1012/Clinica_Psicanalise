package com.psicogest.psicogest.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.SubscriptionStatus;

public record SubscriptionListResponse(
        UUID id,
        Long patientId,
        String patientName,
        UUID subscriptionPlanVersionId,
        String planName,
        String cycle,
        SubscriptionStatus status,
        LocalDate nextCharge,
        boolean cancelAtPeriodEnd,
        Instant createdAt
) {
}
