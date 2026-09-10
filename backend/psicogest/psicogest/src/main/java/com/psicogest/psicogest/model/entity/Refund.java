package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.exception.InvalidFinanceTransitionException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity para reembolsos (refunds)
 * 
 * Ciclo de vida: PENDING → CONFIRMED/FAILED/CANCELLED
 * Append-only: cada refund é imutável, erros geram novos refunds corretivos
 */
@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    private UUID id;

    /**
     * Pagamento a ser reembolsado
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
     * Valor do reembolso
     */
    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    /**
     * Moeda (BRL)
     */
    @Column(
            nullable = false,
            length = 3
    )
    private String currency;

    /**
     * Razão do reembolso
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundReason reason;

    /**
     * Descrição adicional (opcional)
     */
    @Column(length = 255)
    private String description;

    /**
     * Status: PENDING, CONFIRMED, FAILED, CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    /**
     * Chave de idempotência
     */
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    /**
     * Fingerprint criptográfico da requisição
     */
    @Column(
            name = "request_fingerprint",
            updatable = false,
            length = 64
    )
    private String requestFingerprint;

    /**
     * Quando foi solicitado
     */
    @Column(name = "requested_at")
    private Instant requestedAt;

    /**
     * Quando foi confirmado
     */
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /**
     * Quando falhou
     */
    @Column(name = "failed_at")
    private Instant failedAt;

    /**
     * Quando foi cancelado
     */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /**
     * Timestamps
     */
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Versionamento
     */
    @Version
    private Long version;

    /**
     * 7. Confirma reembolso (PENDING → CONFIRMED)
     */
    public void confirm(Instant now) {

        if (status != RefundStatus.PENDING) {

            throw new InvalidFinanceTransitionException(
                    "Somente refunds pendentes podem ser confirmados"
            );
        }

        this.status = RefundStatus.CONFIRMED;

        this.confirmedAt = now;

        this.updatedAt = now;
    }

    /**
     * 7. Marca reembolso como falho (PENDING → FAILED)
     */
    public void fail(Instant now) {

        if (status != RefundStatus.PENDING) {

            throw new InvalidFinanceTransitionException(
                    "Somente refunds pendentes podem falhar"
            );
        }

        this.status = RefundStatus.FAILED;

        this.failedAt = now;

        this.updatedAt = now;
    }

    /**
     * 7. Cancela reembolso (PENDING → CANCELLED)
     */
    public void cancel(Instant now) {

        if (status != RefundStatus.PENDING) {

            throw new InvalidFinanceTransitionException(
                    "Somente refunds pendentes podem ser cancelados"
            );
        }

        this.status = RefundStatus.CANCELLED;

        this.cancelledAt = now;

        this.updatedAt = now;
    }

    /**
     * Razão do reembolso
     */
    public enum RefundReason {
        CUSTOMER_REQUEST,
        PAYMENT_ERROR,
        DUPLICATE_CHARGE,
        SERVICE_ISSUE,
        OTHER
    }

    /**
     * Status do reembolso
     */
    public enum RefundStatus {
        PENDING,     // Aguardando confirmação
        CONFIRMED,   // Confirmado
        FAILED,      // Falhou
        CANCELLED    // Cancelado
    }
}
