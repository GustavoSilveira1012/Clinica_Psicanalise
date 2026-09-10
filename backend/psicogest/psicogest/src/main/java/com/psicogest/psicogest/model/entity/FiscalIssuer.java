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

import com.psicogest.psicogest.model.enums.FiscalTaxRegime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Emissor fiscal (clínica como contribuinte)
 */
@Entity
@Table(
    name = "fiscal_issuers",
    indexes = {
        @Index(name = "idx_fiscal_issuer_clinic", columnList = "clinic_id"),
        @Index(name = "idx_fiscal_issuer_active", columnList = "active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalIssuer {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Clínica
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false, updatable = false)
    private Clinic clinic;

    /**
     * CNPJ (clínica como contribuinte)
     */
    @Column(length = 14, nullable = false, unique = true)
    private String cnpj;

    /**
     * Regime tributário padrão
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "default_tax_regime", nullable = false)
    private FiscalTaxRegime defaultTaxRegime;

    /**
     * Ativo/Inativo
     */
    @Column(nullable = false)
    private Boolean active;

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
