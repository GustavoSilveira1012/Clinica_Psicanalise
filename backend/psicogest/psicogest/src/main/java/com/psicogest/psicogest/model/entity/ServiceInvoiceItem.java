package com.psicogest.psicogest.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
@Table(name = "service_invoice_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInvoiceItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private ServiceInvoice invoice;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    @Builder.Default
    private ItemType itemType = ItemType.SERVICE;

    @Column(name = "service_code", length = 50)
    private String serviceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id", updatable = false)
    private Receivable receivable;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum ItemType {
        SERVICE,
        DEDUCTION,
        ADDITIONAL_CHARGE
    }
}
