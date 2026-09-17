package com.psicogest.psicogest.model.entity.saas;

import com.psicogest.psicogest.model.enums.SaasInvoiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saas_invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasInvoice {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SaasSubscription subscription;

    @Column(length = 60)
    private String provider;

    @Column(name = "provider_invoice_id")
    private String providerInvoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SaasInvoiceStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "BRL";

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "hosted_invoice_url", length = 1000)
    private String hostedInvoiceUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
