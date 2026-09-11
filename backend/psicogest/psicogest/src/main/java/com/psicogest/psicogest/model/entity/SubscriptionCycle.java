package com.psicogest.psicogest.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.SubscriptionCycleStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscription_cycles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCycle {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false, updatable = false)
    private PatientSubscription subscription;

    @Column(name = "cycle_number", nullable = false, updatable = false)
    private Integer cycleNumber;

    @Column(name = "period_start", nullable = false, updatable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false, updatable = false)
    private LocalDate periodEnd;

    @Column(name = "billing_date", nullable = false, updatable = false)
    private LocalDate billingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SubscriptionCycleStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id")
    private Receivable receivable;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_package_id")
    private PatientPackage patientPackage;

    @Column(name = "billed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal billedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "billed_at")
    private Instant billedAt;

    @Column(name = "entitlement_granted_at")
    private Instant entitlementGrantedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public void markEntitlementGranted(PatientPackage patientPackage, Instant now) {
        this.patientPackage = patientPackage;
        this.entitlementGrantedAt = now;
        this.status = SubscriptionCycleStatus.ENTITLEMENT_GRANTED;
        this.updatedAt = now;
    }

    public void markPastDue(Instant now) {
        this.status = SubscriptionCycleStatus.PAST_DUE;
        this.updatedAt = now;
    }

    public void close(Instant now) {
        this.status = SubscriptionCycleStatus.CLOSED;
        this.closedAt = now;
        this.updatedAt = now;
    }
}
