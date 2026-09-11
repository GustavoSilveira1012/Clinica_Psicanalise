package com.psicogest.psicogest.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.PatientPackageSource;
import com.psicogest.psicogest.model.enums.PatientPackageStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "patient_packages",
        indexes = {
                @Index(name = "idx_patient_package_patient", columnList = "patient_id"),
                @Index(name = "idx_patient_package_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientPackage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    @Column(name = "financial_entity_id", nullable = false, updatable = false)
    private UUID financialEntityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_plan_version_id", nullable = false, updatable = false)
    private PackagePlanVersion packagePlanVersion;

    @Column(name = "receivable_id", updatable = false)
    private UUID receivableId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PatientPackageStatus status;

    @Column(name = "purchased_at", nullable = false, updatable = false)
    private Instant purchasedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "exhausted_at")
    private Instant exhaustedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private PatientPackageSource source = PatientPackageSource.ONE_TIME_PURCHASE;

    @Column(name = "purchase_amount", precision = 19, scale = 2)
    private BigDecimal purchaseAmount;

    @Column(name = "subscription_cycle_id", updatable = false)
    private UUID subscriptionCycleId;

    /** Compatibilidade com a versão antiga; o saldo oficial é o ledger. */
    @Transient
    private Long availableSessions;

    public LocalDate getExpirationDate() {
        return expiresAt == null ? null : expiresAt.atZone(ZoneOffset.UTC).toLocalDate();
    }

    public LocalDate getActivationDate() {
        return activatedAt == null ? null : activatedAt.atZone(ZoneOffset.UTC).toLocalDate();
    }

    public void consumeSession() {
        if (availableSessions != null && availableSessions <= 0) {
            throw new IllegalStateException("Sem sessões disponíveis");
        }
        if (availableSessions != null) {
            availableSessions--;
        }
    }

    public void reverseSession() {
        if (availableSessions != null) {
            availableSessions++;
        }
    }

    public void activate(Instant now) {
        status = PatientPackageStatus.ACTIVE;
        activatedAt = now;
        updatedAt = now;
    }

    public void cancel(Instant now) {
        status = PatientPackageStatus.CANCELLED;
        cancelledAt = now;
        updatedAt = now;
    }
}
