package com.psicogest.psicogest.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.BillingInterval;
import com.psicogest.psicogest.model.enums.SubscriptionCancellationPolicy;
import com.psicogest.psicogest.model.enums.SubscriptionGrantPolicy;
import com.psicogest.psicogest.model.enums.SubscriptionPlanVersionStatus;
import com.psicogest.psicogest.model.enums.SubscriptionRolloverPolicy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscription_plan_versions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanVersion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_plan_id", nullable = false, updatable = false)
    private SubscriptionPlan subscriptionPlan;

    @Column(nullable = false, updatable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionPlanVersionStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entitlement_package_version_id", nullable = false, updatable = false)
    private PackagePlanVersion entitlementPackageVersion;

    @Column(name = "cycle_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal cyclePrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, length = 30)
    private BillingInterval billingInterval;

    @Column(name = "interval_count", nullable = false)
    private Integer intervalCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_policy", nullable = false, length = 40)
    private SubscriptionGrantPolicy grantPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "rollover_policy", nullable = false, length = 40)
    private SubscriptionRolloverPolicy rolloverPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_policy", nullable = false, length = 40)
    private SubscriptionCancellationPolicy cancellationPolicy;

    @Column(name = "grace_days", nullable = false)
    private Integer graceDays;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public void publish(Instant now) {
        if (status != SubscriptionPlanVersionStatus.DRAFT) {
            throw new IllegalStateException("Somente versão em rascunho pode ser publicada");
        }
        status = SubscriptionPlanVersionStatus.PUBLISHED;
        createdAt = createdAt == null ? now : createdAt;
    }

    public boolean isPublished() {
        return status == SubscriptionPlanVersionStatus.PUBLISHED;
    }
}
