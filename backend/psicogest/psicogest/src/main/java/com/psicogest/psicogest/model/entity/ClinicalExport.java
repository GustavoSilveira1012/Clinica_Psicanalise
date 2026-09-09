package com.psicogest.psicogest.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 24. Entity para rastreamento de exports clínicos
 * 
 * Ciclo de vida:
 * REQUESTED → PROCESSING → READY → (downloaded)
 * ou
 * REQUESTED → PROCESSING → FAILED
 */
@Entity
@Table(name = "clinical_exports")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalExport {

    @Id
    private UUID id;

    /**
     * Quem solicitou o export
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_psychoanalyst_id", nullable = false, updatable = false)
    private Psychoanalyst requesterPsychoanalyst;

    /**
     * Paciente do qual exportar dados
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    /**
     * Status do export
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClinicalExportStatus status;

    /**
     * Formato do arquivo
     */
    @Column(nullable = false, length = 30)
    private String format;

    /**
     * Flags de conteúdo
     */
    @Column(nullable = false)
    private boolean includeMedicalRecords;

    @Column(nullable = false)
    private boolean includeAddendums;

    @Column(nullable = false)
    private boolean includeAppointments;

    /**
     * Intervalo de datas
     */
    @Column(name = "from_date")
    private java.time.LocalDate fromDate;

    @Column(name = "to_date")
    private java.time.LocalDate toDate;

    /**
     * Chave de storage (após READY)
     */
    @Column(name = "storage_key")
    private String storageKey;

    /**
     * SHA-256 do arquivo (verificação de integridade)
     */
    @Column(name = "file_sha256")
    private String fileSha256;

    /**
     * Tamanho em bytes
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * Motivo da falha (se FAILED)
     */
    @Column(name = "failure_reason")
    private String failureReason;

    /**
     * Timestamps
     */
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_downloaded_at")
    private Instant lastDownloadedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Versionamento
     */
    @Version
    @Column(nullable = false)
    private Long version;

    /**
     * Enumeração de status
     */
    public enum ClinicalExportStatus {
        REQUESTED,    // Solicitação recebida
        PROCESSING,   // Sendo processado
        READY,        // Pronto para download
        FAILED,       // Falhou no processamento
        EXPIRED       // Expirou (90 dias)
    }
}
