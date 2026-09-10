package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.medicalrecord.MedicalRecordRevisionResponseDTO;
import com.psicogest.psicogest.dto.medicalrecord.MedicalRecordRevisionSummaryDTO;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.infrastructure.exfiltration.ClinicalAccessDetector;
import com.psicogest.psicogest.model.entity.MedicalRecord;
import com.psicogest.psicogest.model.entity.MedicalRecordRevision;
import com.psicogest.psicogest.model.entity.Psychoanalyst;
import com.psicogest.psicogest.repository.MedicalRecordRevisionRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;
import com.psicogest.psicogest.security.crypto.ApplicationEncryptionService;
import com.psicogest.psicogest.security.crypto.EncryptionContext;
import com.psicogest.psicogest.security.crypto.EncryptedEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service para gerenciar snapshots de revisões de prontuários
 * 
 * 9. Contexto criptográfico: "MEDICAL_RECORD_REVISION"
 *    - Cada revisão tem sua própria DEK
 *    - Novo IV para cada revisão
 *    - Novo ciphertext mesmo que conteúdo seja igual
 * 
 * 10. Helper para criar snapshot imutável da revisão
 */
@Slf4j
@Service
public class MedicalRecordRevisionService {

    private final MedicalRecordRevisionRepository revisionRepository;
    private final ApplicationEncryptionService encryptionService;
    private final AuditService auditService;
    private final ClinicalAccessDetector accessDetector;

    public MedicalRecordRevisionService(
            MedicalRecordRevisionRepository revisionRepository,
            ApplicationEncryptionService encryptionService,
            AuditService auditService,
            ClinicalAccessDetector accessDetector
    ) {
        this.revisionRepository = revisionRepository;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
        this.accessDetector = accessDetector;
    }

    /**
     * Cria snapshot imutável de uma revisão
     * 
     * 9. Contexto separado: "MEDICAL_RECORD_REVISION"
     *    Garante que IV/DEK/ciphertext são únicos mesmo para conteúdo idêntico
     * 
     * @param record MedicalRecord (já salvo)
     * @param author Psychoanalyst que gerou a revisão
     * @param revisionNumber Número sequencial (incrementado no MedicalRecord)
     * @param plaintext Conteúdo em plaintext (será cifrado aqui)
     * @return MedicalRecordRevision persistida
     */
    @Transactional
    public MedicalRecordRevision createSnapshot(
            MedicalRecord record,
            Psychoanalyst author,
            long revisionNumber,
            String plaintext
    ) {
        UUID revisionId = UUID.randomUUID();

        // 9. Context separado: "MEDICAL_RECORD_REVISION"
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD_REVISION",
                revisionId.toString(),
                "content",
                Map.of("patientId", record.getPatient().getId().toString())
        );

        // Criptografa com DEK nova
        EncryptedEnvelope encrypted = encryptionService.encrypt(
                plaintext,
                context
        );

        // Cria entidade de revisão
        MedicalRecordRevision revision = MedicalRecordRevision.builder()
                .id(revisionId)
                .medicalRecord(record)
                .revisionNumber(revisionNumber)
                .authorPsychoanalyst(author)
                .encryptedContent(encrypted.ciphertext())
                .contentIv(encrypted.iv())
                .encryptedDek(encrypted.wrappedDataKey())
                .cryptoVersion(encrypted.cryptoVersion())
                .cryptoAlgorithm(encrypted.algorithm())
                .keyId(encrypted.keyId())
                .createdAt(Instant.now())
                .build();

        MedicalRecordRevision saved = revisionRepository.saveAndFlush(revision);

        log.info("Snapshot de revisão criado: revisionId={}, recordId={}, revisionNumber={}, author={}",
                saved.getId(), record.getId(), revisionNumber, author.getId());

        return saved;
    }

    /**
     * 17. Listar revisões de um prontuário (metadados apenas)
     * 
     * Retorna timeline em ordem descrescente de revisionNumber
     */
    @Transactional(readOnly = true)
    public List<MedicalRecordRevisionSummaryDTO> findByMedicalRecord(UUID medicalRecordId) {
        List<MedicalRecordRevision> revisions = revisionRepository
                .findByMedicalRecordIdOrderByRevisionNumberDesc(medicalRecordId);

        return revisions.stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    /**
     * 21-22. Ler revisão completa com conteúdo descriptografado
     * 
     * Acesso restrito: apenas autor original (canReadMedicalRecordRevision)
     * 22. Audit obrigatório: recordSensitiveRead + recordClinicalAccess
     */
    @Transactional(readOnly = true)
    public MedicalRecordRevisionResponseDTO findRevision(
            UUID revisionId,
            SecurityActor actor
    ) {
        MedicalRecordRevision revision = revisionRepository
                .findById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Revisão não encontrada"));

        // 21. Context: "MEDICAL_RECORD_REVISION"
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD_REVISION",
                revision.getId().toString(),
                "content",
                Map.of("patientId", revision.getMedicalRecord().getPatient().getId().toString())
        );

        EncryptedEnvelope envelope = new EncryptedEnvelope(
                revision.getCryptoVersion(),
                revision.getCryptoAlgorithm(),
                revision.getKeyId(),
                revision.getEncryptedDek(),
                revision.getContentIv(),
                revision.getEncryptedContent()
        );

        String content = encryptionService.decrypt(envelope, context);

        // 22. Audit obrigatório
        auditService.recordSensitiveRead(new AuditCommand(
                actor.userId(),
                actor.sessionId(),
                AuditAction.MEDICAL_RECORD_REVISION_READ,
                "MEDICAL_RECORD_REVISION",
                revision.getId().toString(),
                revision.getMedicalRecord().getPatient().getId(),
                null,
                AuditOutcome.SUCCESS,
                actor.correlationId(),
                actor.sourceIp(),
                actor.userAgentHash(),
                Map.of(
                        "medicalRecordId", revision.getMedicalRecord().getId().toString(),
                        "revisionNumber", String.valueOf(revision.getRevisionNumber())
                )
        ));

        accessDetector.recordClinicalAccess(
                actor.userId(),
                actor.sessionId(),
                revision.getMedicalRecord().getPatient().getId(),
                actor.sourceIp()
        );

        log.info("Revisão lida: revisionId={}, recordId={}, revisionNumber={}, psychoanalystId={}, correlationId={}",
                revision.getId(), revision.getMedicalRecord().getId(), revision.getRevisionNumber(),
                revision.getAuthorPsychoanalyst().getId(), actor.correlationId());

        return toResponseDTO(revision, content);
    }

    private MedicalRecordRevisionSummaryDTO toSummaryDTO(MedicalRecordRevision revision) {
        return new MedicalRecordRevisionSummaryDTO(
                revision.getId(),
                revision.getRevisionNumber(),
                revision.getAuthorPsychoanalyst().getId(),
                revision.getAuthorPsychoanalyst().getUser().getName(),
                revision.getCreatedAt()
        );
    }

    private MedicalRecordRevisionResponseDTO toResponseDTO(
            MedicalRecordRevision revision,
            String plainContent
    ) {
        return new MedicalRecordRevisionResponseDTO(
                revision.getId(),
                revision.getMedicalRecord().getId(),
                revision.getRevisionNumber(),
                revision.getAuthorPsychoanalyst().getId(),
                revision.getAuthorPsychoanalyst().getUser().getName(),
                plainContent,
                revision.getCreatedAt()
        );
    }
}
