package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.infrastructure.bank.parser.BankTransactionDirection;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Transação bancária importada de extrato
 * 
 * Description é sensível (contém PII: nomes, CPF)
 * Armazenado criptografado com ApplicationEncryptionService
 * 
 * Deduplicação:
 * 1. external_id (ID do banco, se disponível)
 * 2. transaction_fingerprint (hash de múltiplas informações)
 */
@Entity
@Table(
        name = "bank_transactions",
        indexes = {
                @Index(name = "idx_bank_account_booking_date", columnList = "bank_account_id,booking_date"),
                @Index(name = "idx_bank_account_status", columnList = "bank_account_id,status"),
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_bank_transaction_fingerprint",
                        columnNames = {"bank_account_id", "transaction_fingerprint"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransaction {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Conta bancária associada
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id", nullable = false, updatable = false)
    private BankAccount bankAccount;

    /**
     * ID externo do banco (OFX FITID, etc)
     * Pode ser null se não disponível
     */
    @Column(name = "external_transaction_id", length = 255)
    private String externalTransactionId;

    /**
     * Crédito ou débito
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankTransactionDirection direction;

    /**
     * Valor da transação
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Moeda (BRL, USD, etc)
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * Data de lançamento
     */
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    /**
     * Timestamp exato (se disponível)
     */
    @Column(name = "posted_at")
    private Instant postedAt;

    /**
     * Descrição do lançamento (criptografada)
     * Pode conter: PIX RECEBIDO JOÃO DA SILVA CPF 123.456.789-00
     * Armazenado em ciphertext
     */
    @Column(name = "encrypted_description")
    private byte[] encryptedDescription;

    /**
     * IV para descriptografia
     */
    @Column(name = "description_iv")
    private byte[] descriptionIv;

    /**
     * Data Encryption Key envolvida (criptografada)
     */
    @Column(name = "description_encrypted_dek")
    private byte[] descriptionEncryptedDek;

    /**
     * Versão do algoritmo criptográfico
     */
    @Column(name = "crypto_version")
    private Integer cryptoVersion;

    /**
     * Algoritmo usado (AES-256-GCM)
     */
    @Column(name = "crypto_algorithm", length = 30)
    private String cryptoAlgorithm;

    /**
     * ID da chave mestra
     */
    @Column(name = "key_id", length = 255)
    private String keyId;

    /**
     * Referência adicional (código de transação)
     */
    @Column(name = "reference", length = 255)
    private String reference;

    /**
     * Fingerprint SHA-256 para deduplicação
     * = SHA-256(bookingDate + amount + currency + direction + description + reference + externalId)
     */
    @Column(name = "transaction_fingerprint", nullable = false, length = 64)
    private String transactionFingerprint;

    /**
     * Status de reconciliação
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReconciliationStatus status = ReconciliationStatus.UNRECONCILED;

    /**
     * Marcado para ignorar (tarifa, transferência, aporte, juros)
     */
    @Column(name = "ignored_at")
    private Instant ignoredAt;

    /**
     * Usuário que ignorou
     */
    @Column(name = "ignored_by")
    private Long ignoredBy;

    /**
     * Motivo da ignoração
     */
    @Column(name = "ignore_reason", length = 255)
    private String ignoreReason;

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
     * Status de reconciliação
     */
    public enum ReconciliationStatus {
        /**
         * Não conciliado
         */
        UNRECONCILED,

        /**
         * Parcialmente conciliado
         */
        PARTIALLY_RECONCILED,

        /**
         * Totalmente conciliado
         */
        RECONCILED,

        /**
         * Questionado (divergência encontrada)
         */
        DISPUTED
    }

    /**
     * Marca como totalmente reconciliado
     */
    public void markReconciled() {
        this.status = ReconciliationStatus.RECONCILED;
        this.updatedAt = Instant.now();
    }

    /**
     * Marca como parcialmente reconciliado
     */
    public void markPartiallyReconciled() {
        this.status = ReconciliationStatus.PARTIALLY_RECONCILED;
        this.updatedAt = Instant.now();
    }
}
