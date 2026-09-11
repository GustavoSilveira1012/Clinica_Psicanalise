package com.psicogest.psicogest.model.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.SubscriptionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patient_subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientSubscription {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    @Column(name = "financial_entity_id", nullable = false, updatable = false)
    private UUID financialEntityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_plan_version_id", nullable = false, updatable = false)
    private SubscriptionPlanVersion subscriptionPlanVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionStatus status;

    @Column(name = "starts_on", nullable = false, updatable = false)
    private LocalDate startsOn;

    @Column(name = "current_period_start")
    private LocalDate currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDate currentPeriodEnd;

    @Column(name = "next_cycle_start")
    private LocalDate nextCycleStart;

    @Column(name = "billing_anchor_day", nullable = false, updatable = false)
    private Integer billingAnchorDay;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(name = "cancellation_requested_at")
    private Instant cancellationRequestedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public static PatientSubscription create(
            UUID id,
            Patient patient,
            UUID financialEntityId,
            SubscriptionPlanVersion planVersion,
            LocalDate startsOn,
            Instant now
    ) {
        int anchorDay = startsOn.getDayOfMonth();
        SubscriptionStatus initialStatus = startsOn.isAfter(now.atZone(java.time.ZoneOffset.UTC).toLocalDate())
                ? SubscriptionStatus.PENDING_START
                : SubscriptionStatus.ACTIVE;

        return PatientSubscription.builder()
                .id(id)
                .patient(patient)
                .financialEntityId(financialEntityId)
                .subscriptionPlanVersion(planVersion)
                .status(initialStatus)
                .startsOn(startsOn)
                .nextCycleStart(startsOn)
                .billingAnchorDay(anchorDay)
                .cancelAtPeriodEnd(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void requestCancellationAtPeriodEnd(Instant now) {
        if (status == SubscriptionStatus.CANCELLED) {
            return;
        }
        cancelAtPeriodEnd = true;
        cancellationRequestedAt = now;
        updatedAt = now;
    }

    public void cancel(Instant now) {
        status = SubscriptionStatus.CANCELLED;
        cancelAtPeriodEnd = false;
        cancelledAt = now;
        updatedAt = now;
    }

    public void pause(Instant now) {
        if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.PAST_DUE) {
            throw new IllegalStateException("Assinatura não pode ser pausada no estado atual");
        }
        status = SubscriptionStatus.PAUSED;
        pausedAt = now;
        updatedAt = now;
    }

    public void resume(LocalDate resumeDate, Instant now) {
        if (status != SubscriptionStatus.PAUSED) {
            throw new IllegalStateException("Somente assinatura pausada pode ser retomada");
        }
        status = SubscriptionStatus.ACTIVE;
        nextCycleStart = resumeDate;
        billingAnchorDay = resumeDate.getDayOfMonth();
        pausedAt = null;
        updatedAt = now;
    }

    public void advancePeriod(LocalDate periodStart, LocalDate periodEnd, LocalDate nextStart, Instant now) {
        currentPeriodStart = periodStart;
        currentPeriodEnd = periodEnd;
        nextCycleStart = nextStart;
        updatedAt = now;
    }
}
