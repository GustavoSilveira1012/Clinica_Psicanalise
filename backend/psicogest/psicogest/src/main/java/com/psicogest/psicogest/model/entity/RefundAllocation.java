package com.psicogest.psicogest.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity para rastreamento de reembolsos por allocation
 * 
 * Mapeia: Refund → PaymentAllocation (qual allocation foi reembolsada)
 * Append-only, imutável
 */
@Entity
@Table(
        name = "refund_allocations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_refund_allocation",
                        columnNames = {
                                "refund_id",
                                "payment_allocation_id"
                        }
                )
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundAllocation {

    @Id
    private UUID id;

    /**
     * Reembolso
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "refund_id",
            nullable = false,
            updatable = false
    )
    private Refund refund;

    /**
     * Alocação de pagamento sendo reembolsada
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "payment_allocation_id",
            nullable = false,
            updatable = false
    )
    private PaymentAllocation paymentAllocation;

    /**
     * Valor reembolsado desta allocation
     */
    @Column(
            nullable = false,
            precision = 19,
            scale = 2,
            updatable = false
    )
    private BigDecimal amount;

    /**
     * Timestamp
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;
}
