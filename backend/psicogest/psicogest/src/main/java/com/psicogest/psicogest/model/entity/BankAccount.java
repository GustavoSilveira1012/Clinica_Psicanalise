package com.psicogest.psicogest.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Conta bancária da clínica
 * 
 * Associa a clínica com sua conta no banco para importação de extratos
 */
@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Clínica proprietária
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false, updatable = false)
    private Clinic clinic;

    /**
     * Nome da conta (ex: "Operacional", "Reserva")
     */
    @Column(nullable = false, length = 100)
    private String accountName;

    /**
     * Código do banco (3 dígitos)
     */
    @Column(nullable = false, length = 3)
    private String bankCode;

    /**
     * Agência
     */
    @Column(nullable = false, length = 10)
    private String branch;

    /**
     * Número da conta
     */
    @Column(nullable = false, length = 30)
    private String accountNumber;

    /**
     * CPF/CNPJ titular
     */
    @Column(nullable = false, length = 20)
    private String accountHolder;

    /**
     * Ativa ou não
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

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
}
