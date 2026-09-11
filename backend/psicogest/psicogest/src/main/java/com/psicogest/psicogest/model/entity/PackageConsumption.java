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

import com.psicogest.psicogest.model.enums.PackageConsumptionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Consumo de sessão de pacote
 * 
 * Representa uma sessão efetivamente consumida em um agendamento
 * Rastreia: qual appointment, qual pacote, qual item, status (ACTIVE/REVERSED)
 * 
 * Funciona em conjunto com SessionCreditEntry (ledger)
 * Permite rastrear não apenas o saldo, mas QUAL sessão foi consumida
 */
@Entity
@Table(
    name = "package_consumptions",
    indexes = {
        @Index(name = "idx_consumption_appointment", columnList = "appointment_id"),
        @Index(name = "idx_consumption_package", columnList = "patient_package_id"),
        @Index(name = "idx_consumption_item", columnList = "package_item_id"),
        @Index(name = "idx_consumption_status", columnList = "status"),
        @Index(name = "idx_consumption_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageConsumption {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Agendamento que consumiu a sessão
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, updatable = false)
    private Appointment appointment;

    /**
     * Pacote do qual foi consumida a sessão
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_package_id", nullable = false, updatable = false)
    private PatientPackage patientPackage;

    /**
     * Item específico do pacote
     * (ex: "Sessão de Psicanálise" vs "Sessão Avançada")
     */
    @Column(name = "package_item_id", nullable = false, updatable = false)
    private UUID packageItemId;

    /**
     * Status da consumção
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageConsumptionStatus status;

    /**
     * Data/hora do consumo
     */
    @Column(name = "consumed_at", nullable = false, updatable = false)
    private Instant consumedAt;

    /**
     * Data/hora da reversão (se aplicável)
     */
    @Column(name = "reversed_at")
    private Instant reversedAt;

    /**
     * Motivo da reversão
     */
    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

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
     * Reverte o consumo
     */
    public void reverse(String reason, Instant now) {
        if (this.status == PackageConsumptionStatus.REVERSED) {
            throw new IllegalStateException(
                "Consumo já foi revertido"
            );
        }
        this.status = PackageConsumptionStatus.REVERSED;
        this.reversedAt = now;
        this.reversalReason = reason;
        this.updatedAt = now;
    }
}
