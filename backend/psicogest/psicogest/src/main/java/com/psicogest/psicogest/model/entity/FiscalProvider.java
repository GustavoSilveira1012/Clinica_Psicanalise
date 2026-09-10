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

import com.psicogest.psicogest.model.enums.FiscalEnvironment;
import com.psicogest.psicogest.model.enums.FiscalProviderType;
import com.psicogest.psicogest.model.enums.FiscalTaxRegime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Provedor fiscal configurado para uma clínica
 * 
 * Armazena credenciais encriptadas e configurações
 * de acesso aos serviços de emissão de NFS-e
 */
@Entity
@Table(
    name = "fiscal_providers",
    indexes = {
        @Index(name = "idx_fiscal_provider_clinic", columnList = "clinic_id"),
        @Index(name = "idx_fiscal_provider_type", columnList = "provider_type"),
        @Index(name = "idx_fiscal_provider_active", columnList = "active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalProvider {

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
     * Tipo de provedor
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
     * Regime tributário
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", nullable = false)
    private FiscalTaxRegime taxRegime;

    /**
     * URL base do endpoint do provedor
     */
    @Column(name = "endpoint_url", nullable = false, length = 500)
    private String endpointUrl;

    /**
     * Credenciais encriptadas (JSON encriptado com AES-256-GCM)
     * Contém: cnpj, username, password, token, etc
     */
    @Column(name = "encrypted_credentials", nullable = false, columnDefinition = "TEXT")
    private String encryptedCredentials;

    /**
     * IV (Initialization Vector) para decriptografia
     */
    @Column(name = "credentials_iv", nullable = false, length = 24)
    private String credentialsIv;

    /**
     * ID da chave criptográfica usada
     */
    @Column(name = "credentials_key_id", nullable = false, length = 100)
    private String credentialsKeyId;

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
