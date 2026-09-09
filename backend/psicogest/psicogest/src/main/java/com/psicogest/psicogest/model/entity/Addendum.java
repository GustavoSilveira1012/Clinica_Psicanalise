package com.psicogest.psicogest.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Addendum - Anotação complementar a um prontuário finalizado
 * 
 * Características:
 * - Apenas o autor original do prontuário pode adicionar
 * - Prontuário deve estar FINALIZED
 * - Cada addendum tem criptografia independente (context: "MEDICAL_RECORD_ADDENDUM")
 * - Prevents ciphertext swap attacks via GCM-AAD
 */
@Entity
@Table(name = "addendums")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Addendum {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_psychoanalyst_id", nullable = false)
    private Psychoanalyst authorPsychoanalyst;

    /**
     * Conteúdo criptografado do addendum (AES-256-GCM)
     */
    @Column(name = "encrypted_content", nullable = false, columnDefinition = "bytea")
    private byte[] encryptedContent;

    /**
     * IV (Initialization Vector) para AES-256-GCM
     */
    @Column(name = "content_iv", nullable = false, columnDefinition = "bytea")
    private byte[] contentIv;

    /**
     * DEK (Data Encryption Key) envolvido com o KEK
     */
    @Column(name = "encrypted_dek", nullable = false, columnDefinition = "bytea")
    private byte[] encryptedDek;

    /**
     * Versão do algoritmo de criptografia (para compatibilidade futura)
     */
    @Column(name = "crypto_version", nullable = false)
    private Integer cryptoVersion;

    /**
     * Algoritmo criptográfico (ex: "AES_256_GCM")
     */
    @Column(name = "crypto_algorithm", nullable = false)
    private String cryptoAlgorithm;

    /**
     * ID da chave mestre (KEK) usada
     */
    @Column(name = "key_id", nullable = false)
    private String keyId;

    /**
     * Código de motivo do addendum (não-sensível)
     * Exemplos: CLARIFICATION, CORRECTION, COMPLEMENT, OTHER
     * Descrição clínica fica no conteúdo cifrado
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "reason_code",
            nullable = false,
            updatable = false,
            length = 50
    )
    private com.psicogest.psicogest.model.enums.MedicalRecordAddendumReason reason;

    /**
     * Controle de concorrência otimista
     */
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Timestamp de criação
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp de última atualização
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Metadados adicionais (opcional)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private java.util.Map<String, Object> metadata;

    /**
     * Pré-persistência: define timestamps e ID
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.version == null) {
            this.version = 0L;
        }
    }

    /**
     * Pré-atualização: atualiza timestamp
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
        this.version++;
    }
}
