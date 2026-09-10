package com.psicogest.psicogest.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 9. Conta de crédito do paciente
 * 
 * Saldo nunca é persistido - sempre calculado via CreditEntries
 * Representa crédito disponível para aplicar em futuras cobranças
 */
@Entity
@Table(name = "credit_accounts")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditAccount {

    @Id
    private UUID id;

    /**
     * Paciente proprietário do crédito
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
     * Clínica do contexto (opcional, para segregação)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "clinic_id",
            updatable = false
    )
    private Clinic clinic;

    /**
     * Moeda (BRL)
     */
    @Column(
            nullable = false,
            length = 3,
            updatable = false
    )
    private String currency;

    /**
     * Quando foi criada
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    /**
     * Versionamento (OCC)
     */
    @Version
    private Long version;
}
