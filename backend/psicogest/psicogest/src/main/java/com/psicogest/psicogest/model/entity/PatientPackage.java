package com.psicogest.psicogest.model.entity;

import java.time.Instant;
import java.time.LocalDate;
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

import com.psicogest.psicogest.model.enums.PatientPackageStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Instância de pacote adquirido por paciente
 * 
 * Representa uma compra: "Paciente X comprou o plano Y versão Z"
 * Imutável após criação
 */
@Entity
@Table(
    name = "patient_packages",
    indexes = {
        @Index(name = "idx_patient_package_patient", columnList = "patient_id"),
        @Index(name = "idx_patient_package_status", columnList = "status"),
        @Index(name = "idx_patient_package_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientPackage {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Paciente que comprou
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    /**
     * Entidade financeira (clínica/unidade) associada ao pacote
     * Usado para encontrar pacotes elegíveis ao consumir
     */
    @Column(name = "financial_entity_id", nullable = false, updatable = false)
    private Long financialEntityId;

    /**
     * Versão do plano no momento da compra (armazenada como UUID para referência)
     * A versão é imutável e referencia o snapshot dessa época
     */
    @Column(name = "package_plan_version_id", nullable = false, updatable = false)
    private UUID packagePlanVersionId;

    /**
     * Status do pacote
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatientPackageStatus status;

    /**
     * Data de ativação
     */
    @Column(name = "activation_date")
    private LocalDate activationDate;

    /**
     * Data de expiração
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * Sessões ainda disponíveis (saldo)
     */
    @Column(nullable = false)
    private Long availableSessions;

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
     * Consome uma sessão
     */
    public void consumeSession() {
        if (this.availableSessions <= 0) {
            throw new IllegalStateException(
                "Sem sessões disponíveis"
            );
        }
        this.availableSessions--;
    }

    /**
     * Reversa consumo
     */
    public void reverseSession() {
        this.availableSessions++;
    }
}
