package com.psicogest.psicogest.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 15. Entity para alocação de pagamentos
 * 
 * Vincula um pagamento a uma ou mais contas a receber
 * Permite que um único pagamento cubra múltiplas contas (ou parte delas)
 * 
 * Append-oriented: nunca edita histórico
 * Erros financeiros → operação corretiva explícita futura
 */
@Entity
@Table(
        name = "payment_allocations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_payment_receivable_allocation",
                        columnNames = {
                                "payment_id",
                                "receivable_id"
                        }
                )
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAllocation {

    /**
     * Identificador único da alocação
     */
    @Id
    private UUID id;

    /**
     * Pagamento sendo alocado
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "payment_id",
            nullable = false,
            updatable = false
    )
    private Payment payment;

    /**
     * Conta a receber sendo paga
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "receivable_id",
            nullable = false,
            updatable = false
    )
    private Receivable receivable;

    /**
     * Valor alocado (pode ser menor que o valor total da conta)
     * Permite pagamentos parciais
     */
    @Column(
            nullable = false,
            precision = 19,
            scale = 2,
            updatable = false
    )
    private BigDecimal amount;

    /**
     * Quando a alocação foi criada
     * Imutável (append-oriented)
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;
}
