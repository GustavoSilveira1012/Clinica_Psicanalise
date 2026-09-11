package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.BillingInterval;
import com.psicogest.psicogest.model.enums.SubscriptionCancellationPolicy;
import com.psicogest.psicogest.model.enums.SubscriptionGrantPolicy;
import com.psicogest.psicogest.model.enums.SubscriptionPlanVersionStatus;
import com.psicogest.psicogest.model.enums.SubscriptionRolloverPolicy;

public record SubscriptionPlanVersionResponse(
        UUID id,
        UUID subscriptionPlanId,
        Integer version,
        SubscriptionPlanVersionStatus status,
        UUID entitlementPackageVersionId,
        BigDecimal cyclePrice,
        String currency,
        BillingInterval billingInterval,
        Integer intervalCount,
        SubscriptionGrantPolicy grantPolicy,
        SubscriptionRolloverPolicy rolloverPolicy,
        SubscriptionCancellationPolicy cancellationPolicy,
        Integer graceDays,
        LocalDate effectiveFrom
) {
}
