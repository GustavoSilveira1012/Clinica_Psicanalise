package com.psicogest.psicogest.model.entity;

import java.math.BigDecimal;
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

import com.psicogest.psicogest.model.enums.FiscalEnvironment;
import com.psicogest.psicogest.model.enums.FiscalProviderType;
import com.psicogest.psicogest.model.enums.FiscalTaxRegime;
import com.psicogest.psicogest.model.enums.ServiceInvoiceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nota Fiscal de Serviço Eletrônica (NFS-e)
 * 
 * Representa uma fatura de serviços emitida pela clínica
 * para fins fiscais (ABRASF, provedores municipais, etc)
 */
@Entity
@Table(
    name = "service_invoices",
    indexes = {
        @Index(name = "idx_service_invoice_clinic", columnList = "clinic_id"),
        @Index(name = "idx_service_invoice_status", columnList = "status"),
        @Index(name = "idx_service_invoice_nfse_id", columnList = "nfse_id"),
        @Index(name = "idx_service_invoice_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceInvoice {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Clínica emissora
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false, updatable = false)
    private Clinic clinic;

    /**
     * Tipo de provedor fiscal
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private FiscalProviderType provider;

    /**
     * Ambiente (homologação ou produção)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private FiscalEnvironment environment;

    /**
     * Regime tributário
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private FiscalTaxRegime taxRegime;

    /**
     * Status da nota fiscal
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceInvoiceStatus status;

    /**
     * Número sequencial da nota (gerado pela prefeitura/provedor)
     */
    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    /**
     * ID da NFS-e no provedor fiscal
     */
    @Column(name = "nfse_id", length = 100, unique = true)
    private String nfseId;

    /**
     * Valor bruto (antes de deduções)
     */
    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    /**
     * Deduções (inss, irrf, pis, cofins, etc)
     */
    @Column(name = "deductions", nullable = false, precision = 19, scale = 2)
    private BigDecimal deductions;

    /**
     * Valor líquido (gross - deductions)
     */
    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    /**
     * Moeda
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * Timestamps
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Motivo de rejeição/erro
     */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /**
     * Versionamento
     */
    @Version
    private Long version;

    /**
     * Marca como pendente (DRAFT → PENDING)
     */
    public void markPending(Instant now) {
        if (this.status != ServiceInvoiceStatus.DRAFT) {
            throw new IllegalStateException(
                "Apenas rascunhos podem ser marcados como pendentes"
            );
        }
        this.status = ServiceInvoiceStatus.PENDING;
        this.updatedAt = now;
    }

    /**
     * Marca como processando (PENDING → PROCESSING)
     */
    public void markProcessing(Instant now) {
        if (this.status != ServiceInvoiceStatus.PENDING) {
            throw new IllegalStateException(
                "Apenas pendentes podem ser marcadas como processando"
            );
        }
        this.status = ServiceInvoiceStatus.PROCESSING;
        this.submittedAt = now;
        this.updatedAt = now;
    }

    /**
     * Marca como autorizada (PROCESSING → AUTHORIZED)
     */
    public void markAuthorized(Instant now) {
        if (this.status != ServiceInvoiceStatus.PROCESSING) {
            throw new IllegalStateException(
                "Apenas em processamento podem ser autorizadas"
            );
        }
        this.status = ServiceInvoiceStatus.AUTHORIZED;
        this.authorizedAt = now;
        this.updatedAt = now;
    }

    /**
     * Marca como rejeitada (PROCESSING → REJECTED)
     */
    public void markRejected(Instant now, String reason) {
        if (this.status != ServiceInvoiceStatus.PROCESSING) {
            throw new IllegalStateException(
                "Apenas em processamento podem ser rejeitadas"
            );
        }
        this.status = ServiceInvoiceStatus.REJECTED;
        this.rejectedAt = now;
        this.rejectionReason = reason;
        this.updatedAt = now;
    }

    /**
     * Marca como erro (→ ERROR)
     */
    public void markError(Instant now, String reason) {
        this.status = ServiceInvoiceStatus.ERROR;
        this.rejectionReason = reason;
        this.updatedAt = now;
    }

    /**
     * Marca para cancelamento (AUTHORIZED → CANCEL_PENDING)
     */
    public void markCancelPending(Instant now) {
        if (this.status != ServiceInvoiceStatus.AUTHORIZED) {
            throw new IllegalStateException(
                "Apenas autorizadas podem ser canceladas"
            );
        }
        this.status = ServiceInvoiceStatus.CANCEL_PENDING;
        this.updatedAt = now;
    }

    /**
     * Marca como cancelada (CANCEL_PENDING → CANCELLED)
     */
    public void markCancelled(Instant now) {
        if (this.status != ServiceInvoiceStatus.CANCEL_PENDING) {
            throw new IllegalStateException(
                "Apenas aguardando cancelamento podem ser canceladas"
            );
        }
        this.status = ServiceInvoiceStatus.CANCELLED;
        this.cancelledAt = now;
        this.updatedAt = now;
    }

    /**
     * Marca como requerendo reconciliação (AUTHORIZED → RECONCILIATION_REQUIRED)
     */
    public void markReconciliationRequired(Instant now) {
        if (this.status != ServiceInvoiceStatus.AUTHORIZED) {
            throw new IllegalStateException(
                "Apenas autorizadas podem requer reconciliação"
            );
        }
        this.status = ServiceInvoiceStatus.RECONCILIATION_REQUIRED;
        this.updatedAt = now;
    }

    /**
     * Prepara invoice para emissão (calcula DPS número, snapshot de taxas)
     * DRAFT → PENDING
     */
    public void prepareForIssue(
        long dpsNumber,
        Object taxSnapshot,
        Instant now
    ) {
        if (this.status != ServiceInvoiceStatus.DRAFT) {
            throw new IllegalStateException(
                "Apenas rascunhos podem ser preparados para emissão"
            );
        }
        this.status = ServiceInvoiceStatus.PENDING;
        this.updatedAt = now;
    }

    public ServiceInvoice orElseThrow(Object object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'orElseThrow'");
    }
}
