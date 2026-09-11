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

import com.psicogest.psicogest.model.enums.SessionCreditDirection;
import com.psicogest.psicogest.model.enums.SessionCreditEntryType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de movimento de crédito de sessão
 * 
 * Rastreia toda a história de adições e consums de créditos
 * Imutável após criação
 */
@Entity
@Table(
    name = "session_credit_entries",
    indexes = {
        @Index(name = "idx_credit_entry_patient", columnList = "patient_id"),
        @Index(name = "idx_credit_entry_package", columnList = "patient_package_id"),
        @Index(name = "idx_credit_entry_type", columnList = "entry_type"),
        @Index(name = "idx_credit_entry_direction", columnList = "direction"),
        @Index(name = "idx_credit_entry_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCreditEntry {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Paciente afetado
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    /**
     * Pacote de sessão (se aplicável)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_package_id", updatable = false)
    private PatientPackage patientPackage;

    /**
     * Tipo de evento
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private SessionCreditEntryType entryType;

    /**
     * Direção (CREDIT ou DEBIT)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private SessionCreditDirection direction;

    /**
     * Quantidade de sessões movimentadas
     */
    @Column(nullable = false, updatable = false)
    private Long sessionCount;

    /**
     * Motivo (para MANUAL_ADJUSTMENT)
     */
    @Column(name = "reason", length = 500, updatable = false)
    private String reason;

    /**
     * Referência a agendamento (se consumo/reversão)
     */
    @Column(name = "appointment_id", updatable = false)
    private UUID appointmentId;

    /**
     * Item do pacote (se aplicável)
     * Permite rastrear qual tipo de sessão foi consumida
     */
    @Column(name = "package_item_id", updatable = false)
    private UUID packageItemId;

    /**
     * Criado em
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
