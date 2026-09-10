package com.psicogest.psicogest.model.entity;

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
