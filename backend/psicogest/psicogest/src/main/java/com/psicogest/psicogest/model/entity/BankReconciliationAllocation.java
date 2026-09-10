package com.psicogest.psicogest.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Alocação de reconciliação entre transação bancária e payment/refund
 * 
 * Permite:
 * - Alocação parcial (um lançamento → múltiplos documents)
 * - Múltiplas alocações (múltiplos lançamentos → um document)
 * - Reversão de alocações (status REVERTED)
 * 
 * BankTransaction (1) ← → (*) BankReconciliationAllocation → (1) Payment ou (1) Refund
 */
@Entity
@Table(
        name = "bank_reconciliation_allocations",
        indexes = {
                @Index(name = "idx_bank_transaction_allocation", columnList = "bank_transaction_id"),
                @Index(name = "idx_payment_allocation", columnList = "payment_id"),
                @Index(name = "idx_refund_allocation", columnList = "refund_id"),
                @Index(name = "idx_settlement_allocation", columnList = "provider_settlement_id"),
                @Index(name = "idx_allocation_status", columnList = "status"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankReconciliationAllocation {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Lançamento bancário alocado
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_transaction_id", nullable = false, updatable = false)
    private BankTransaction bankTransaction;

    /**
     * Pagamento alocado (pode ser null se for refund)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", updatable = false)
    private Payment payment;

    /**
     * Reembolso alocado (pode ser null se for payment)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", updatable = false)
    private Refund refund;

    /**
     * Repasse de gateway alocado (pode ser null se for payment/refund)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_settlement_id", updatable = false)
    private ProviderSettlement providerSettlement;

    /**
     * Valor alocado (pode ser parcial do lançamento)
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal allocatedAmount;

    /**
     * Moeda
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * Status de alocação
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status;

    /**
     * Quem fez a alocação
     * - AUTOMATIC: algoritmo sugeriu
     * - MANUAL: usuário confirmou manualmente
     * - SYSTEM: sistema corrigiu
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AllocationSource allocatedBy = AllocationSource.MANUAL;

    /**
     * Quando foi alocado
     */
    @Column(name = "allocated_at", nullable = false)
    private Instant allocatedAt;

    /**
     * Timestamps
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Versionamento
     */
    @Version
    private Long version;

    /**
     * Status de alocação
     */
    public enum AllocationStatus {
        /**
         * Sugerida automaticamente (await confirmação)
         */
        SUGGESTED,

        /**
         * Confirmada manualmente
         */
        CONFIRMED,

        /**
         * Revertida (transação foi "desfeita")
         */
        REVERTED,

        /**
         * Questionada (divergência encontrada)
         */
        DISPUTED
    }

    /**
     * Fonte de alocação
     */
    public enum AllocationSource {
        /**
         * Algoritmo automático sugeriu
         */
        AUTOMATIC,

        /**
         * Usuário confirmou manualmente
         */
        MANUAL,

        /**
         * Sistema corrigiu automaticamente
         */
        SYSTEM
    }
}
