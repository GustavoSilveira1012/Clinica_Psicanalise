package com.psicogest.psicogest.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.psicogest.psicogest.model.enums.FiscalOperationType;
import com.psicogest.psicogest.model.enums.FiscalOperationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Operação fiscal (emissão, cancelamento, substituição)
 * 
 * Rastreia chamadas ao provedor e seus resultados
 */
@Entity
@Table(
    name = "fiscal_operations",
    indexes = {
        @Index(name = "idx_fiscal_op_invoice", columnList = "invoice_id"),
        @Index(name = "idx_fiscal_op_type_status", columnList = "operation_type, status"),
        @Index(name = "idx_fiscal_op_idempotency", columnList = "idempotency_key", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalOperation {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Invoice relacionada
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private ServiceInvoice invoice;

    /**
     * Tipo de operação
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, updatable = false)
    private FiscalOperationType operationType;

    /**
     * Status da operação
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FiscalOperationStatus status;

    /**
     * Chave de idempotência (evita duplicação)
     */
    @Column(name = "idempotency_key", length = 100, nullable = false, updatable = false)
    private String idempotencyKey;

    /**
     * Request payload (JSON serializado, pode ser criptografado em produção)
     */
    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    /**
     * Response payload (JSON serializado, nunca armazenar credenciais)
     */
    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    /**
     * Mensagem de erro (se houver)
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Timestamps
     */
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Versionamento
     */
    @Version
    private Long version;
}
