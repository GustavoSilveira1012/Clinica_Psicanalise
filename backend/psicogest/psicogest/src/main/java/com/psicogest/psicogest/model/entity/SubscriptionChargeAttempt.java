package com.psicogest.psicogest.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.SubscriptionChargeAttemptStatus;

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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscription_charge_attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionChargeAttempt {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_cycle_id", nullable = false, updatable = false)
    private SubscriptionCycle subscriptionCycle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(nullable = false, length = 50, updatable = false)
    private String provider;

    @Column(name = "idempotency_key", nullable = false, length = 100, updatable = false)
    private String idempotencyKey;

    @Column(name = "attempt_number", nullable = false, updatable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SubscriptionChargeAttemptStatus status;

    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
