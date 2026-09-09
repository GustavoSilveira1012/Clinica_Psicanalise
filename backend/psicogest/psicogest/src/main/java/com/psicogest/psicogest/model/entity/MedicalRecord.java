package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.MedicalRecordStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table( name = "medical_records" )
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord {

    @Id
    private UUID id;

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

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "therapeutic_relationship_id",
            nullable = false,
            updatable = false
    )
    private TherapeuticRelationship therapeuticRelationship;

    @ManyToOne( fetch = FetchType.LAZY )
    @JoinColumn(
            name = "appointment_id",
            updatable = false
    )
    private Appointment appointment;

    @Enumerated( EnumType.STRING )
    @Column(
            nullable = false,
            length = 30
    )
    private MedicalRecordStatus status;

    @Column(
            name = "encrypted_content",
            nullable = false
    )
    private byte[] encryptedContent;

    @Column(
            name = "content_iv",
            nullable = false
    )
    private byte[] contentIv;

    @Column(
            name = "encrypted_dek",
            nullable = false
    )
    private byte[] encryptedDek;

    @Column(
            name = "crypto_version",
            nullable = false
    )
    private Integer cryptoVersion;

    @Column(
            name = "crypto_algorithm",
            nullable = false,
            length = 30
    )
    private String cryptoAlgorithm;

    @Column(
            name = "key_id",
            nullable = false
    )
    private String keyId;

    @Version
    @Column( nullable = false )
    private Long version;

    @Column(
            name = "current_revision_number",
            nullable = false
    )
    private Long currentRevisionNumber;

    @Column(
            name = "finalized_revision_number"
    )
    private Long finalizedRevisionNumber;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Column( name = "finalized_at" )
    private Instant finalizedAt;

    public void replaceEncryptedContent(
            byte[] encryptedContent,
            byte[] contentIv,
            byte[] encryptedDek,
            Integer cryptoVersion,
            String cryptoAlgorithm,
            String keyId
    ) {

        if (
                status
                == MedicalRecordStatus.FINALIZED
        ) {

            throw new IllegalStateException(
                    "Prontuário finalizado é imutável"
            );
        }

        this.encryptedContent =
                encryptedContent.clone();

        this.contentIv =
                contentIv.clone();

        this.encryptedDek =
                encryptedDek.clone();

        this.cryptoVersion =
                cryptoVersion;

        this.cryptoAlgorithm =
                cryptoAlgorithm;

        this.keyId =
                keyId;

        this.updatedAt =
                Instant.now();
    }

    public void finalizeRecord() {

        if (
                status
                == MedicalRecordStatus.FINALIZED
        ) {

            throw new IllegalStateException(
                    "Prontuário já está finalizado"
            );
        }

        status =
                MedicalRecordStatus.FINALIZED;

        // Registra qual snapshot originou o documento final
        finalizedRevisionNumber =
                currentRevisionNumber;

        finalizedAt =
                Instant.now();

        updatedAt =
                finalizedAt;
    }

    /**
     * Gera próximo número de revisão
     * 
     * Usado em edições concorrentes (aba A + aba B).
     * Com @Version do JPA, conflitos são detectados automaticamente.
     * 
     * @return Próximo número de revisão
     * @throws IllegalStateException Se prontuário está finalizado
     */
    public long nextRevisionNumber() {

        if (
                status
                != MedicalRecordStatus.DRAFT
        ) {

            throw new IllegalStateException(
                    "Prontuário finalizado não pode gerar nova revisão"
            );
        }

        currentRevisionNumber =
                currentRevisionNumber + 1;

        return currentRevisionNumber;
    }
}
