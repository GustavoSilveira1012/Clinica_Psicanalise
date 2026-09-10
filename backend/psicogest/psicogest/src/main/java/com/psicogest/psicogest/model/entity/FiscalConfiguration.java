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

import com.psicogest.psicogest.model.enums.FiscalEnvironment;
import com.psicogest.psicogest.model.enums.FiscalProviderType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuração fiscal por período (competência)
 * 
 * Uma clínica pode ter múltiplas configurações
 * que vigoram em períodos diferentes
 */
@Entity
@Table(
    name = "fiscal_configurations",
    indexes = {
        @Index(name = "idx_fiscal_config_issuer", columnList = "issuer_id"),
        @Index(name = "idx_fiscal_config_period", columnList = "validity_start, validity_end"),
        @Index(name = "idx_fiscal_config_effective", columnList = "issuer_id, validity_start, validity_end")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalConfiguration {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Emissor fiscal
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issuer_id", nullable = false, updatable = false)
    private FiscalIssuer issuer;

    /**
     * Provedor fiscal
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false, updatable = false)
    private FiscalProvider provider;

    /**
     * Tipo de provedor (redundante para queries, opcional)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private FiscalProviderType providerType;

    /**
     * Ambiente (homologação ou produção)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FiscalEnvironment environment;

    /**
     * Versão de layout (NFSe padrão: 2.02, 2.03, etc)
     */
    @Column(name = "layout_version", length = 10)
    private String layoutVersion;

    /**
     * Data de início de validade (01/01/YYYY)
     */
    @Column(name = "validity_start", nullable = false)
    private LocalDate validityStart;

    /**
     * Data de fim de validade (31/12/YYYY, null = indefinido)
     */
    @Column(name = "validity_end")
    private LocalDate validityEnd;

    /**
     * Ativa/Inativa
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
