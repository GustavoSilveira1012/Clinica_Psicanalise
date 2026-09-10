package com.psicogest.psicogest.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 12. Entity para contas a receber (receivables)
 * 
 * Representa faturamento de consultas/serviços
 * Ciclo de vida: OPEN → PARTIALLY_PAID → PAID ou CANCELLED
 */
@Entity
@Table(name = "receivables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receivable {

    @Id
    private UUID id;

    /**
     * Paciente responsável pelo pagamento
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "patient_id",
            nullable = false,
            updatable = false
    )
    private Patient patient;

    /**
     * Consulta relacionada (opcional, pode ser serviço avulso)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "appointment_id",
            updatable = false
    )
    private Appointment appointment;

    /**
     * Descrição do serviço/consulta
     */
    @Column(
            nullable = false,
            length = 255
    )
    private String description;

    /**
     * Valor bruto (sem desconto)
     */
    @Column(
            name = "gross_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal grossAmount;

    /**
     * Desconto aplicado
     */
    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal discountAmount;

    /**
     * Valor líquido (gross - discount)
     * Validado no banco: netAmount = grossAmount - discountAmount
     */
    @Column(
            name = "net_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal netAmount;

    /**
     * Moeda (BRL por enquanto)
     */
    @Column(
            nullable = false,
            length = 3
    )
    private String currency;

    /**
     * Data de vencimento
     */
    @Column(
            name = "due_date",
            nullable = false
    )
    private LocalDate dueDate;

    /**
     * Status: OPEN, PARTIALLY_PAID, PAID, CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceivableStatus status;

    /**
     * Quando foi cancelada (se status = CANCELLED)
     */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /**
     * Motivo do cancelamento
     */
    @Column(
            name = "cancellation_reason"
    )
    private String cancellationReason;

    /**
     * Timestamps
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    /**
     * Versionamento (OCC - Optimistic Concurrency Control)
     */
    @Version
    private Long version;

    /**
     * Status da conta a receber
     */
    public enum ReceivableStatus {
        OPEN,              // Aberta, não paga
        PARTIALLY_PAID,    // Parcialmente paga
        PAID,              // Totalmente paga
        CANCELLED          // Cancelada
    }
}
