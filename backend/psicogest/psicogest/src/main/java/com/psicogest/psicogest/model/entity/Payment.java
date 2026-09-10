package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.exception.InvalidFinanceTransitionException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 14. Entity para pagamentos recebidos
 * 
 * Representa um pagamento único recebido de um paciente
 * Pode ser alocado para múltiplas contas a receber via PaymentAllocation
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    private UUID id;

    /**
     * Paciente que realizou o pagamento
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
     * Clínica responsável pelo pagamento
     * Futuro: será obrigatório, por enquanto opcional
     */
    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "clinic_id",
            updatable = false
    )
    private Clinic clinic;

    /**
     * Valor do pagamento
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
     * Forma de pagamento: PIX, CREDIT_CARD, BANK_TRANSFER, CASH, etc
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false
    )
    private PaymentMethod paymentMethod;

    /**
     * Status: PENDING, CONFIRMED, FAILED, CANCELLED, etc
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /**
     * Gateway/Provider (Stripe, MercadoPago, etc)
     */
    private String provider;

    /**
     * ID da transação no provider
     */
    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    /**
     * Chave de idempotência (deduplicação de pagamentos)
     * Importante para webhooks e retries
     */
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    /**
     * 12. Fingerprint criptográfico da requisição
     * 
     * Garante que mesma idempotency-key = mesma requisição
     * Usa: patientId | amount | paymentMethod | provider | providerTransactionId
     * Protege contra replay com dados diferentes
     */
    @Column(
            name = "request_fingerprint",
            updatable = false,
            length = 64
    )
    private String requestFingerprint;

    /**
     * Quando o pagamento foi confirmado
     */
    @Column(name = "received_at")
    private Instant receivedAt;

    /**
     * Quando falhou (se status = FAILED)
     */
    @Column(name = "failed_at")
    private Instant failedAt;

    /**
     * Quando foi cancelado (se status = CANCELLED)
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
     * Confirma o pagamento (transição PENDING → CONFIRMED)
     */
    public void confirm(Instant receivedAt) {

        if (status != PaymentStatus.PENDING) {

            throw new InvalidFinanceTransitionException(
                    "Somente pagamentos pendentes podem ser confirmados"
            );
        }

        this.status = PaymentStatus.CONFIRMED;

        this.receivedAt = receivedAt;

        this.updatedAt = Instant.now();
    }

    /**
     * Marca o pagamento como falho (transição PENDING → FAILED)
     */
    public void fail() {

        if (status != PaymentStatus.PENDING) {

            throw new InvalidFinanceTransitionException(
                    "Somente pagamentos pendentes podem falhar"
            );
        }

        this.status = PaymentStatus.FAILED;

        this.failedAt = Instant.now();

        this.updatedAt = this.failedAt;
    }

    /**
     * Cancela o pagamento (transição PENDING → CANCELLED)
     */
    public void cancel() {

        if (status != PaymentStatus.PENDING) {

            throw new InvalidFinanceTransitionException(
                    "Somente pagamentos pendentes podem ser cancelados"
            );
        }

        this.status = PaymentStatus.CANCELLED;

        this.cancelledAt = Instant.now();

        this.updatedAt = this.cancelledAt;
    }

    /**
     * Forma de pagamento
     */
    public enum PaymentMethod {
        PIX,
        CREDIT_CARD,
        DEBIT_CARD,
        BANK_TRANSFER,
        CASH,
        CHECK,
        OTHER
    }

    /**
     * Status do pagamento
     */
    public enum PaymentStatus {
        PENDING,               // Aguardando confirmação
        CONFIRMED,             // Confirmado
        FAILED,                // Falhou
        CANCELLED,             // Cancelado
        PARTIALLY_REFUNDED,    // Parcialmente reembolsado
        REFUNDED               // Totalmente reembolsado
    }
}
