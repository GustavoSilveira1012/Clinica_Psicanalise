package com.psicogest.psicogest.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

import com.psicogest.psicogest.model.enums.PackageActivationPolicy;
import com.psicogest.psicogest.model.enums.PackagePlanVersionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "package_plan_versions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackagePlanVersion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_plan_id", nullable = false, updatable = false)
    private PackagePlan packagePlan;

    @Column(nullable = false, updatable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PackagePlanVersionStatus status;

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "activation_policy", nullable = false, length = 40)
    private PackageActivationPolicy activationPolicy;

    @Column(name = "consume_no_show", nullable = false)
    private boolean consumeNoShow;

    @Column(name = "consume_late_cancellation", nullable = false)
    private boolean consumeLateCancellation;

    @Column(name = "late_cancellation_minutes")
    private Integer lateCancellationMinutes;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "packagePlanVersion", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<PackagePlanItem> items = List.of();

    public int totalSessions() {
        return items.stream()
                .map(PackagePlanItem::getQuantity)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public boolean isPublished() {
        return status == PackagePlanVersionStatus.PUBLISHED;
    }
}
