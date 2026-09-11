package com.psicogest.psicogest.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.psicogest.psicogest.model.enums.SubscriptionMandateStatus;

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
@Table(name = "subscription_payment_mandates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPaymentMandate {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false, updatable = false)
    private PatientSubscription subscription;

    @Column(nullable = false, length = 50, updatable = false)
    private String provider;

    @Column(name = "provider_customer_reference", length = 255, updatable = false)
    private String providerCustomerReference;

    @Column(name = "provider_payment_method_reference", nullable = false, length = 255, updatable = false)
    private String providerPaymentMethodReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionMandateStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
