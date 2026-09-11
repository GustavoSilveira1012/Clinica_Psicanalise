package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.SubscriptionCycleStatus;

public record SubscriptionCycleResponse(
        UUID id,
        UUID subscriptionId,
        Integer cycleNumber,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate billingDate,
        SubscriptionCycleStatus status,
        UUID receivableId,
        UUID patientPackageId,
        BigDecimal billedAmount,
        String currency
) {
}
