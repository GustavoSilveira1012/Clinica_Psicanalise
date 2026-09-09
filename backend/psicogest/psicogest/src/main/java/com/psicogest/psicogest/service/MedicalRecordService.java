package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.medicalrecord.MedicalRecordStateMachine;
import com.psicogest.psicogest.dto.medicalrecord.MedicalRecordCreateDTO;
import com.psicogest.psicogest.dto.medicalrecord.MedicalRecordResponseDTO;
import com.psicogest.psicogest.dto.medicalrecord.MedicalRecordSummaryDTO;
import com.psicogest.psicogest.dto.medicalrecord.MedicalRecordUpdateDTO;
import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.exception.MedicalRecordConflictException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.infrastructure.exfiltration.ClinicalAccessDetector;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.MedicalRecordStatus;
import com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus;
import com.psicogest.psicogest.repository.*;
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
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final PsychoanalystRepository psychoanalystRepository;
    private final TherapeuticRelationshipRepository relationshipRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicalEncryptionService encryptionService;
    private final AuditService auditService;
    private final MedicalRecordStateMachine stateMachine;
    private final ClinicalAccessDetector accessDetector;
    private final MedicalRecordRevisionService revisionService;

    public MedicalRecordService(
            MedicalRecordRepository medicalRecordRepository,
            PatientRepository patientRepository,
            PsychoanalystRepository psychoanalystRepository,
            TherapeuticRelationshipRepository relationshipRepository,
            AppointmentRepository appointmentRepository,
            ClinicalEncryptionService encryptionService,
            AuditService auditService,
            MedicalRecordStateMachine stateMachine,
            ClinicalAccessDetector accessDetector,
            MedicalRecordRevisionService revisionService
    ) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientRepository = patientRepository;
        this.psychoanalystRepository = psychoanalystRepository;
        this.relationshipRepository = relationshipRepository;
        this.appointmentRepository = appointmentRepository;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
        this.stateMachine = stateMachine;
        this.accessDetector = accessDetector;
        this.revisionService = revisionService;
    }

    @Transactional
    public MedicalRecordSummaryDTO create(
            Long patientId,
            MedicalRecordCreateDTO dto,
            SecurityActor actor
    ) {
        Psychoanalyst psychoanalyst = psychoanalystRepository
                .findByUserId(actor.userId())
                .orElseThrow(() -> new AccessDeniedException("Acesso negado"));

        Patient patient = patientRepository
                .findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));

        TherapeuticRelationship relationship = relationshipRepository
                .findByPatientIdAndPsychoanalystIdAndStatus(
                        patientId,
                        psychoanalyst.getId(),
                        TherapeuticRelationshipStatus.ACTIVE
                )
                .orElseThrow(() -> new AccessDeniedException(
                        "Não existe vínculo terapêutico ativo com este paciente"
                ));

        Appointment appointment = null;
        if (dto.appointmentId() != null) {
            appointment = appointmentRepository
                    .findById(dto.appointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Consulta não encontrada"));

            if (!appointment.getPatient().getId().equals(patientId)) {
                throw new MedicalRecordConflictException("A consulta não pertence ao paciente");
            }

            if (!appointment.getPsychoanalyst().getId().equals(psychoanalyst.getId())) {
                throw new MedicalRecordConflictException(
                        "A consulta não pertence ao profissional autenticado"
                );
            }

            if (medicalRecordRepository.existsByAppointmentId(appointment.getId())) {
                throw new MedicalRecordConflictException("Esta consulta já possui prontuário");
            }
        }

        UUID recordId = UUID.randomUUID();
        EncryptionContext encryptionContext = new EncryptionContext(
                "MEDICAL_RECORD",
                recordId.toString(),
                patientId,
                "content"
        );

        EncryptedEnvelope encrypted = encryptionService.encrypt(dto.content(), encryptionContext);

        Instant now = Instant.now();
        MedicalRecord record = MedicalRecord.builder()
                .id(recordId)
                .patient(patient)
                .authorPsychoanalyst(psychoanalyst)
                .therapeuticRelationship(relationship)
                .appointment(appointment)
                .status(MedicalRecordStatus.DRAFT)
                .encryptedContent(encrypted.ciphertext())
                .contentIv(encrypted.iv())
                .encryptedDek(encrypted.wrappedDataKey())
                .cryptoVersion(encrypted.cryptoVersion())
                .cryptoAlgorithm(encrypted.algorithm())
                .keyId(encrypted.keyId())
                .version(0L)
                .currentRevisionNumber(1L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        MedicalRecord saved = medicalRecordRepository.saveAndFlush(record);

        // 11. Criar snapshot inicial (revisão 1)
        // Nota: não registra MEDICAL_RECORD_REVISION_CREATED pois a criação é coberta por MEDICAL_RECORD_CREATED
        revisionService.createSnapshot(
                saved,
                psychoanalyst,
                1L,
                dto.content()
        );

        auditService.recordCriticalWrite(new AuditCommand(
                actor.userId(),
                actor.sessionId(),
                AuditAction.MEDICAL_RECORD_CREATED,
                "MEDICAL_RECORD",
                saved.getId().toString(),
                patientId,
                null,
                AuditOutcome.SUCCESS,
                actor.correlationId(),
                actor.sourceIp(),
                actor.userAgentHash(),
                Map.of("status", saved.getStatus().name(), "version", saved.getVersion())
        ));

        log.info("Prontuário criado: recordId={}, patientId={}, psychoanalystId={}, correlationId={}",
                saved.getId(), patientId, psychoanalyst.getId(), actor.correlationId());

        return toSummaryDTO(saved);
    }

    @Transactional(readOnly = true)
    public MedicalRecordResponseDTO findById(
            UUID recordId,
            SecurityActor actor
    ) {
        MedicalRecord record = medicalRecordRepository
                .findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário não encontrado"));

        EncryptionContext context = encryptionContextFor(record);
        EncryptedEnvelope envelope = new EncryptedEnvelope(
                record.getCryptoVersion(),
                record.getCryptoAlgorithm(),
                record.getKeyId(),
                record.getEncryptedDek(),
                record.getContentIv(),
                record.getEncryptedContent()
        );

        String content = encryptionService.decrypt(envelope, context);

        auditService.recordSensitiveRead(new AuditCommand(
                actor.userId(),
                actor.sessionId(),
                AuditAction.MEDICAL_RECORD_READ,
                "MEDICAL_RECORD",
                record.getId().toString(),
                record.getPatient().getId(),
                null,
                AuditOutcome.SUCCESS,
                actor.correlationId(),
                actor.sourceIp(),
                actor.userAgentHash(),
                Map.of("status", record.getStatus().name(), "version", record.getVersion())
        ));

        accessDetector.recordClinicalAccess(
                actor.userId(),
                actor.sessionId(),
                record.getPatient().getId(),
                actor.sourceIp()
        );

        log.info("Prontuário lido: recordId={}, patientId={}, psychoanalystId={}, correlationId={}",
                record.getId(), record.getPatient().getId(), 
                record.getAuthorPsychoanalyst().getId(), actor.correlationId());

        return toResponseDTO(record, content);
    }

    @Transactional
    public MedicalRecordResponseDTO update(
            UUID recordId,
            MedicalRecordUpdateDTO dto,
            SecurityActor actor
    ) {
        MedicalRecord record = medicalRecordRepository
                .findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário não encontrado"));

        if (record.getStatus() == MedicalRecordStatus.FINALIZED) {
            throw new MedicalRecordConflictException("Prontuário finalizado não pode ser alterado");
        }

        Psychoanalyst psychoanalyst = resolvePsychoanalyst(actor.userId());

        if (!record.getAuthorPsychoanalyst().getId().equals(psychoanalyst.getId())) {
            throw new AccessDeniedException("Acesso negado");
        }

        // 13-14. Fluxo de revisão
        // Gera novo número de revisão
        long revisionNumber = record.nextRevisionNumber();

        // Criptografa novo conteúdo para o MedicalRecord atual
        EncryptionContext context = encryptionContextFor(record);
        EncryptedEnvelope encrypted = encryptionService.encrypt(dto.content(), context);

        record.replaceEncryptedContent(
                encrypted.ciphertext(),
                encrypted.iv(),
                encrypted.wrappedDataKey(),
                encrypted.cryptoVersion(),
                encrypted.algorithm(),
                encrypted.keyId()
        );

        MedicalRecord saved = medicalRecordRepository.saveAndFlush(record);

        // Cria snapshot da revisão
        revisionService.createSnapshot(
                record,
                psychoanalyst,
                revisionNumber,
                dto.content()
        );

        // 15. Audit da revisão com revisionNumber
        auditService.recordCriticalWrite(new AuditCommand(
                actor.userId(),
                actor.sessionId(),
                AuditAction.MEDICAL_RECORD_REVISION_CREATED,
                "MEDICAL_RECORD",
                record.getId().toString(),
                record.getPatient().getId(),
                null,
                AuditOutcome.SUCCESS,
                actor.correlationId(),
                actor.sourceIp(),
                actor.userAgentHash(),
                Map.of("revisionNumber", revisionNumber)
        ));

        log.info("Prontuário atualizado com revisão: recordId={}, patientId={}, psychoanalystId={}, revisionNumber={}, correlationId={}",
                saved.getId(), record.getPatient().getId(), psychoanalyst.getId(), revisionNumber, actor.correlationId());

        return toResponseDTO(saved, dto.content());
    }

    @Transactional
    public MedicalRecordResponseDTO finalizeRecord(
            UUID recordId,
            SecurityActor actor
    ) {
        MedicalRecord record = medicalRecordRepository
                .findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário não encontrado"));

        Psychoanalyst psychoanalyst = resolvePsychoanalyst(actor.userId());

        if (!record.getAuthorPsychoanalyst().getId().equals(psychoanalyst.getId())) {
            throw new AccessDeniedException("Acesso negado");
        }

        stateMachine.validateTransition(record.getStatus(), MedicalRecordStatus.FINALIZED);

        record.finalizeRecord();

        MedicalRecord saved = medicalRecordRepository.saveAndFlush(record);

        auditService.recordCriticalWrite(new AuditCommand(
                actor.userId(),
                actor.sessionId(),
                AuditAction.MEDICAL_RECORD_FINALIZED,
                "MEDICAL_RECORD",
                record.getId().toString(),
                record.getPatient().getId(),
                null,
                AuditOutcome.SUCCESS,
                actor.correlationId(),
                actor.sourceIp(),
                actor.userAgentHash(),
                Map.of("version", saved.getVersion())
        ));

        log.info("Prontuário finalizado: recordId={}, patientId={}, psychoanalystId={}, correlationId={}",
                saved.getId(), record.getPatient().getId(), psychoanalyst.getId(), actor.correlationId());

        EncryptionContext context = encryptionContextFor(record);
        EncryptedEnvelope envelope = new EncryptedEnvelope(
                record.getCryptoVersion(),
                record.getCryptoAlgorithm(),
                record.getKeyId(),
                record.getEncryptedDek(),
                record.getContentIv(),
                record.getEncryptedContent()
        );

        String content = encryptionService.decrypt(envelope, context);

        return toResponseDTO(saved, content);
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordSummaryDTO> findByPatientId(
            Long patientId,
            SecurityActor actor
    ) {
        Patient patient = patientRepository
                .findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));

        List<MedicalRecord> records = medicalRecordRepository
                .findByPatientOrderByCreatedAtDesc(patient);

        return records.stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    private MedicalRecordSummaryDTO toSummaryDTO(MedicalRecord record) {
        return new MedicalRecordSummaryDTO(
                record.getId(),
                record.getAuthorPsychoanalyst().getId(),
                record.getAuthorPsychoanalyst().getUser().getName(),
                record.getStatus(),
                record.getCreatedAt(),
                record.getFinalizedAt()
        );
    }

    private Psychoanalyst resolvePsychoanalyst(Long userId) {
        return psychoanalystRepository
                .findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Acesso negado"));
    }

    private EncryptionContext encryptionContextFor(MedicalRecord record) {
        return new EncryptionContext(
                "MEDICAL_RECORD",
                record.getId().toString(),
                record.getPatient().getId(),
                "content"
        );
    }

    private MedicalRecordResponseDTO toResponseDTO(
            MedicalRecord record,
            String plainContent
    ) {
        return new MedicalRecordResponseDTO(
                record.getId(),
                record.getPatient().getId(),
                record.getAuthorPsychoanalyst().getId(),
                record.getTherapeuticRelationship().getId(),
                record.getAppointment() != null ? record.getAppointment().getId() : null,
                record.getStatus(),
                plainContent,
                record.getCryptoVersion(),
                record.getCryptoAlgorithm(),
                record.getKeyId(),
                record.getVersion(),
                record.getCreatedAt(),
                record.getUpdatedAt(),
                record.getFinalizedAt()
        );
    }
}
