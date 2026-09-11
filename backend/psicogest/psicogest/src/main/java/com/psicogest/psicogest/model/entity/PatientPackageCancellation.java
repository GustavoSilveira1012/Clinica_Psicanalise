package com.psicogest.psicogest.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.psicogest.psicogest.model.enums.PackageCancellationReason;
import com.psicogest.psicogest.model.enums.PackageCancellationSettlementMode;
import com.psicogest.psicogest.model.enums.PackageCancellationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cancelamento de pacote de sessão
 * 
 * Registra o histórico completo de cancelamento de um pacote:
 * - Motivo e modo de liquidação
 * - Valores calculados (snapshot)
 * - Status de processamento
 * - Quem e quando solicitou
 * 
 * Todos os campos são imutáveis após criação.
 */
@Entity
@Table(
    name = "patient_package_cancellations",
    indexes = {
        @Index(name = "idx_package_cancellation_package", columnList = "patient_package_id"),
        @Index(name = "idx_package_cancellation_status", columnList = "status"),
        @Index(name = "idx_package_cancellation_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientPackageCancellation {

    /**
     * ID único do cancelamento
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Pacote cancelado
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_package_id", nullable = false, updatable = false)
    private PatientPackage patientPackage;

    /**
     * Modo de liquidação
     */
    @Enumerated
    @Column(nullable = false, updatable = false)
    private PackageCancellationSettlementMode settlementMode;

    /**
     * Motivo do cancelamento
     */
    @Enumerated
    @Column(nullable = false, updatable = false)
    private PackageCancellationReason reason;

    /**
     * Status do cancelamento
     */
    @Enumerated
    @Column(nullable = false)
    private PackageCancellationStatus status;

    /**
     * Versão do algoritmo de cálculo
     * 
     * Permite rastrear mudanças futuras na lógica
     */
    @Column(name = "calculation_version", length = 50, nullable = false, updatable = false)
    private String calculationVersion;

    /**
     * Snapshots dos valores calculados
     * Todos os valores abaixo são imutáveis e representam o estado no momento do cancelamento
     */

    @Column(name = "original_package_amount", precision = 19, scale = 2, nullable = false, updatable = false)
    private BigDecimal originalPackageAmount;

    @Column(name = "consumed_value", precision = 19, scale = 2, nullable = false, updatable = false)
    private BigDecimal consumedValue;

    @Column(name = "remaining_service_value", precision = 19, scale = 2, nullable = false, updatable = false)
    private BigDecimal remainingServiceValue;

    @Column(name = "paid_amount_before", precision = 19, scale = 2, nullable = false, updatable = false)
    private BigDecimal paidAmountBefore;

    @Column(name = "receivable_adjustment_amount", precision = 19, scale = 2, nullable = false, updatable = false)
    private BigDecimal receivableAdjustmentAmount;

    @Column(name = "settlement_amount", precision = 19, scale = 2, nullable = false, updatable = false)
    private BigDecimal settlementAmount;

    @Column(name = "outstanding_consumed_amount", precision = 19, scale = 2, nullable = false, updatable = false)
    private BigDecimal outstandingConsumedAmount;

    /**
     * Quem solicitou o cancelamento
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false, updatable = false)
    private User requestedBy;

    /**
     * Quando foi solicitado
     */
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

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
    @Column(name = "version")
    private Long version;

    /**
     * Marca o cancelamento como completo
     * 
     * @param completedAt Instante de conclusão
     */
    public void complete(Instant completedAt) {
        this.status = PackageCancellationStatus.COMPLETED;
        this.updatedAt = completedAt;
    }
}
