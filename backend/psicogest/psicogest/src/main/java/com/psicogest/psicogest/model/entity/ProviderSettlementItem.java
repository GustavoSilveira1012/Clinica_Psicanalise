package com.psicogest.psicogest.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

import com.psicogest.psicogest.model.enums.ProviderSettlementItemType;
import com.psicogest.psicogest.model.enums.SettlementEntryDirection;

/**
 * Item de um repasse
 * 
 * Pode ser:
 * - PAYMENT (aumenta saldo)
 * - REFUND (diminui saldo)
 * - PROVIDER_FEE (diminui saldo)
 * - CHARGEBACK (diminui saldo)
 * - ADJUSTMENT (pode ser ambos)
 */
@Entity
@Table(name = "provider_settlement_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderSettlementItem {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Repasse que contém este item
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "settlement_id",
            nullable = false,
            updatable = false
    )
    private ProviderSettlement settlement;

    /**
     * ID do item no gateway (se aplicável)
     */
    @Column(
            name = "provider_item_id",
            updatable = false,
            length = 100
    )
    private String providerItemId;

    /**
     * Tipo de item
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "item_type",
            nullable = false,
            updatable = false
    )
    private ProviderSettlementItemType itemType;

    /**
     * Direção (crédito/débito) neste item
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "direction",
            nullable = false,
            updatable = false
    )
    private SettlementEntryDirection direction;

    /**
     * Valor do item
     */
    @Column(
            nullable = false,
            precision = 19,
            scale = 2,
            updatable = false
    )
    private BigDecimal amount;

    /**
     * Payment associado (se PAYMENT)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "payment_id",
            updatable = false
    )
    private Payment payment;

    /**
     * Refund associado (se REFUND)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "refund_id",
            updatable = false
    )
    private Refund refund;

    /**
     * Referência externa (código no gateway)
     */
    @Column(
            name = "external_reference",
            updatable = false,
            length = 255
    )
    private String externalReference;

    /**
     * Descrição do item (para fees, chargebacks, ajustes)
     */
    @Column(
            name = "description",
            updatable = false,
            length = 500
    )
    private String description;

    /**
     * Timestamps
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;
}
