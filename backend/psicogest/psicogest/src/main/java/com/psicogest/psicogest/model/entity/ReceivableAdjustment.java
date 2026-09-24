package com.psicogest.psicogest.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.psicogest.psicogest.model.enums.ReceivableAdjustmentDirection;
import com.psicogest.psicogest.model.enums.ReceivableAdjustmentType;

/**
 * Ajuste de cobrança (receivable)
 * 
 * Registra ajustes (aumentos ou diminuições) no valor de uma cobrança.
 * Exemplos: multa por atraso, juros, desconto, abatimento.
 * 
 * Cada ajuste é imutável e auditado.
 */
@Entity
@org.hibernate.annotations.Immutable
@Table(
    name = "receivable_adjustments",
    indexes = {
        @Index(name = "idx_receivable_adjustment_receivable_id", columnList = "receivable_id"),
        @Index(name = "idx_receivable_adjustment_created_at", columnList = "created_at")
    }
)
public class ReceivableAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private ReceivableAdjustmentDirection direction;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason_code", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, updatable = false)
    private ReceivableAdjustmentType adjustmentType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
    private User createdBy;

    // Constructors
    public ReceivableAdjustment() {
        this.createdAt = Instant.now();
    }

    public ReceivableAdjustment(
            Receivable receivable,
            ReceivableAdjustmentDirection direction,
            BigDecimal amount,
            String reason,
            ReceivableAdjustmentType adjustmentType,
            User createdBy
    ) {
        this.receivable = receivable;
        this.direction = direction;
        this.amount = amount;
        this.reason = reason;
        this.adjustmentType = java.util.Objects.requireNonNull(adjustmentType);
        this.createdBy = java.util.Objects.requireNonNull(createdBy);
        this.createdAt = Instant.now();
    }

    // Getters e setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Receivable getReceivable() {
        return receivable;
    }

    public void setReceivable(Receivable receivable) {
        this.receivable = receivable;
    }

    public ReceivableAdjustmentDirection getDirection() {
        return direction;
    }

    public void setDirection(ReceivableAdjustmentDirection direction) {
        this.direction = direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public ReceivableAdjustmentType getAdjustmentType() { return adjustmentType; }

    public User getCreatedBy() { return createdBy; }
}
