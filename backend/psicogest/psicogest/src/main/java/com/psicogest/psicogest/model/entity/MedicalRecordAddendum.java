package com.psicogest.psicogest.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "medical_record_addendums")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordAddendum {

    @Id
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "medical_record_id",
            nullable = false,
            updatable = false
    )
    private MedicalRecord medicalRecord;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "author_psychoanalyst_id",
            nullable = false,
            updatable = false
    )
    private Psychoanalyst authorPsychoanalyst;

    @Column(
            name = "encrypted_content",
            nullable = false,
            updatable = false
    )
    private byte[] encryptedContent;

    @Column(
            name = "content_iv",
            nullable = false,
            updatable = false
    )
    private byte[] contentIv;

    @Column(
            name = "encrypted_dek",
            nullable = false,
            updatable = false
    )
    private byte[] encryptedDek;

    @Column(
            name = "crypto_version",
            nullable = false,
            updatable = false
    )
    private Integer cryptoVersion;

    @Column(
            name = "crypto_algorithm",
            nullable = false,
            updatable = false
    )
    private String cryptoAlgorithm;

    @Column(
            name = "key_id",
            nullable = false,
            updatable = false
    )
    private String keyId;

    @Column(
            nullable = false,
            length = 500,
            updatable = false
    )
    private String reason;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;
}