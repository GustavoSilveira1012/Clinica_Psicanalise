package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.BillingInterval;
import com.psicogest.psicogest.model.enums.SubscriptionCancellationPolicy;
import com.psicogest.psicogest.model.enums.SubscriptionGrantPolicy;
import com.psicogest.psicogest.model.enums.SubscriptionRolloverPolicy;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateSubscriptionPlanVersionRequest(
        @NotNull UUID entitlementPackageVersionId,
        @NotNull @DecimalMin("0.01") BigDecimal cyclePrice,
        @NotNull BillingInterval billingInterval,
        @NotNull @Min(1) Integer intervalCount,
        @NotNull SubscriptionGrantPolicy grantPolicy,
        @NotNull SubscriptionRolloverPolicy rolloverPolicy,
        @NotNull SubscriptionCancellationPolicy cancellationPolicy,
        @NotNull @Min(0) Integer graceDays,
        @NotNull LocalDate effectiveFrom
) {
}
