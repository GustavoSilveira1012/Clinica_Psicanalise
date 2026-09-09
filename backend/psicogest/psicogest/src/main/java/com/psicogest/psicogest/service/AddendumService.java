package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.AddendumCreateDTO;
import com.psicogest.psicogest.dto.AddendumResponseDTO;
import com.psicogest.psicogest.dto.AddendumSummaryDTO;
import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.exception.MedicalRecordConflictException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.infrastructure.exfiltration.ClinicalAccessDetector;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.MedicalRecordStatus;
import com.psicogest.psicogest.repository.AddendumRepository;
import com.psicogest.psicogest.repository.MedicalRecordRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;
import com.psicogest.psicogest.security.crypto.ClinicalEncryptionService;
import com.psicogest.psicogest.security.crypto.EncryptionContext;
import com.psicogest.psicogest.security.crypto.EncryptedEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AddendumService {

    private final AddendumRepository addendumRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PsychoanalystRepository psychoanalystRepository;
    private final ClinicalEncryptionService encryptionService;
    private final AuditService auditService;
    private final ClinicalAccessDetector accessDetector;

    public AddendumService(
            AddendumRepository addendumRepository,
            MedicalRecordRepository medicalRecordRepository,
            PsychoanalystRepository psychoanalystRepository,
            ClinicalEncryptionService encryptionService,
            AuditService auditService,
            ClinicalAccessDetector accessDetector
    ) {
        this.addendumRepository = addendumRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.psychoanalystRepository = psychoanalystRepository;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
        this.accessDetector = accessDetector;
    }

    /**
     * 14-19. Criar addendum em prontuário finalizado
     * 
     * - Apenas autor original pode adicionar
     * - Prontuário deve estar FINALIZED
     * - Context: "MEDICAL_RECORD_ADDENDUM" (distinct from MedicalRecord)
     * - Defesa em profundidade: verifica autor mesmo após @PreAuthorize
     */
    @Transactional
    public AddendumResponseDTO create(
            UUID medicalRecordId,
            AddendumCreateDTO dto,
            SecurityActor actor
    ) {
        // Buscar prontuário
        MedicalRecord record = medicalRecordRepository
                .findById(medicalRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário não encontrado"));

        // Validação: prontuário deve estar FINALIZED
        if (record.getStatus() != MedicalRecordStatus.FINALIZED) {
            throw new MedicalRecordConflictException(
                    "Somente prontuários finalizados podem receber complemento"
            );
        }

        // Resolver autor
        Psychoanalyst psychoanalyst = psychoanalystRepository
                .findByUserId(actor.userId())
                .orElseThrow(() -> new AccessDeniedException("Acesso negado"));

        // Defesa em profundidade: verificar autoria original
        if (!record.getAuthorPsychoanalyst().getId().equals(psychoanalyst.getId())) {
            throw new AccessDeniedException("Acesso negado");
        }

        // Gerar ID antes da criptografia
        UUID addendumId = UUID.randomUUID();

        // Context separado: "MEDICAL_RECORD_ADDENDUM"
        EncryptionContext encryptionContext = new EncryptionContext(
                "MEDICAL_RECORD_ADDENDUM",
                addendumId.toString(),
                record.getPatient().getId(),
                "content"
        );

        // Criptografar
        EncryptedEnvelope encrypted = encryptionService.encrypt(
                dto.content(),
                encryptionContext
        );

        // Persistir
        Instant now = Instant.now();
        Addendum addendum = Addendum.builder()
                .id(addendumId)
                .medicalRecord(record)
                .authorPsychoanalyst(psychoanalyst)
                .encryptedContent(encrypted.ciphertext())
                .contentIv(encrypted.iv())
                .encryptedDek(encrypted.wrappedDataKey())
                .cryptoVersion(encrypted.cryptoVersion())
                .cryptoAlgorithm(encrypted.algorithm())
                .keyId(encrypted.keyId())
                .reason(dto.reason())
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        Addendum saved = addendumRepository.saveAndFlush(addendum);

        // AuditLog na mesma transaction
        auditService.recordCriticalWrite(new AuditCommand(
                actor.userId(),
                actor.sessionId(),
                AuditAction.MEDICAL_RECORD_ADDENDUM_CREATED,
                "MEDICAL_RECORD_ADDENDUM",
                saved.getId().toString(),
                record.getPatient().getId(),
                null,
                AuditOutcome.SUCCESS,
                actor.correlationId(),
                actor.sourceIp(),
                actor.userAgentHash(),
                Map.of(
                        "medicalRecordId", record.getId().toString(),
                        "reason", dto.reason().name()
                )
        ));

        log.info("Addendum criado: addendumId={}, recordId={}, psychoanalystId={}, correlationId={}",
                saved.getId(), medicalRecordId, psychoanalyst.getId(), actor.correlationId());

        return toResponseDTO(saved, dto.content());
    }

    /**
     * Ler addendum completo (com content descriptografado)
     */
    @Transactional(readOnly = true)
    public AddendumResponseDTO findById(
            UUID addendumId,
            SecurityActor actor
    ) {
        Addendum addendum = addendumRepository
                .findById(addendumId)
                .orElseThrow(() -> new ResourceNotFoundException("Addendum não encontrado"));

        // Context separado: "MEDICAL_RECORD_ADDENDUM"
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD_ADDENDUM",
                addendum.getId().toString(),
                addendum.getMedicalRecord().getPatient().getId(),
                "content"
        );

        EncryptedEnvelope envelope = new EncryptedEnvelope(
                addendum.getCryptoVersion(),
                addendum.getCryptoAlgorithm(),
                addendum.getKeyId(),
                addendum.getEncryptedDek(),
                addendum.getContentIv(),
                addendum.getEncryptedContent()
        );

        String content = encryptionService.decrypt(envelope, context);

        // Audit: leitura sensível
        auditService.recordSensitiveRead(new AuditCommand(
                actor.userId(),
                actor.sessionId(),
                AuditAction.MEDICAL_RECORD_ADDENDUM_READ,
                "MEDICAL_RECORD_ADDENDUM",
                addendum.getId().toString(),
                addendum.getMedicalRecord().getPatient().getId(),
                null,
                AuditOutcome.SUCCESS,
                actor.correlationId(),
                actor.sourceIp(),
                actor.userAgentHash(),
                Map.of("medicalRecordId", addendum.getMedicalRecord().getId().toString())
        ));

        accessDetector.recordClinicalAccess(
                actor.userId(),
                actor.sessionId(),
                addendum.getMedicalRecord().getPatient().getId(),
                actor.sourceIp()
        );

        log.info("Addendum lido: addendumId={}, recordId={}, psychoanalystId={}, correlationId={}",
                addendum.getId(), addendum.getMedicalRecord().getId(),
                addendum.getAuthorPsychoanalyst().getId(), actor.correlationId());

        return toResponseDTO(addendum, content);
    }

    /**
     * 31. Listar addendums de prontuário (metadados apenas, sem content)
     * Retorna timeline para evitar múltiplas descriptografias
     */
    @Transactional(readOnly = true)
    public List<AddendumSummaryDTO> findByMedicalRecord(
            UUID medicalRecordId,
            SecurityActor actor
    ) {
        MedicalRecord record = medicalRecordRepository
                .findById(medicalRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário não encontrado"));

        List<Addendum> addendums = addendumRepository
                .findByMedicalRecordOrderByCreatedAtDesc(record);

        return addendums.stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    private Psychoanalyst resolvePsychoanalyst(Long userId) {
        return psychoanalystRepository
                .findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Acesso negado"));
    }

    private AddendumResponseDTO toResponseDTO(
            Addendum addendum,
            String plainContent
    ) {
        return new AddendumResponseDTO(
                addendum.getId(),
                addendum.getMedicalRecord().getId(),
                addendum.getAuthorPsychoanalyst().getId(),
                addendum.getAuthorPsychoanalyst().getUser().getName(),
                addendum.getReason(),
                plainContent,
                addendum.getCreatedAt()
        );
    }

    private AddendumSummaryDTO toSummaryDTO(Addendum addendum) {
        return new AddendumSummaryDTO(
                addendum.getId(),
                addendum.getAuthorPsychoanalyst().getUser().getName(),
                addendum.getReason(),
                addendum.getCreatedAt()
        );
    }
}
