package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.CreditEntryDirection;
import com.psicogest.psicogest.model.enums.CreditEntryType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 10. Entrada de crédito no ledger
 * 
 * Cada movimento é imutável e rastreável
 * Saldo calculado pela soma CREDIT - DEBIT
 */
@Entity
@Table(name = "credit_entries")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditEntry {

    @Id
    private UUID id;

    /**
     * Conta de crédito
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "credit_account_id",
            nullable = false,
            updatable = false
    )
    private CreditAccount creditAccount;

    /**
     * Direção: CREDIT (entrada) ou DEBIT (saída)
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            updatable = false
    )
    private CreditEntryDirection direction;

    /**
     * Tipo: origem do movimento
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "entry_type",
            nullable = false,
            updatable = false
    )
    private CreditEntryType entryType;

    /**
     * Valor do movimento
     */
    @Column(
            nullable = false,
            precision = 19,
            scale = 2,
            updatable = false
    )
    private BigDecimal amount;

    /**
     * Cobrança de origem (para cancelamento)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "source_receivable_id",
            updatable = false
    )
    private Receivable sourceReceivable;

    /**
     * Alocação de origem (para cancelamento)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "source_payment_allocation_id",
            updatable = false
    )
    private PaymentAllocation sourcePaymentAllocation;

    /**
     * Cobrança alvo (para aplicação de crédito)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "target_receivable_id",
            updatable = false
    )
    private Receivable targetReceivable;

    /**
     * Cancelamento associado (para rastreabilidade)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "receivable_cancellation_id",
            updatable = false
    )
    private ReceivableCancellation cancellation;

    /**
     * Quem criou esta entrada
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by_user_id",
            nullable = false,
            updatable = false
    )
    private User createdBy;

    /**
     * Quando foi criada
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;
}
