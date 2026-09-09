package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.medicalrecord.MedicalRecordStateMachine;
import com.psicogest.psicogest.dto.MedicalRecordCreateDTO;
import com.psicogest.psicogest.dto.MedicalRecordResponseDTO;
import com.psicogest.psicogest.dto.MedicalRecordUpdateDTO;
import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.exception.MedicalRecordConflictException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.infrastructure.exfiltration.ClinicalAccessDetector;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.AuditAction;
import com.psicogest.psicogest.model.enums.AuditOutcome;
import com.psicogest.psicogest.model.enums.MedicalRecordStatus;
import com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus;
import com.psicogest.psicogest.repository.*;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.crypto.ClinicalEncryptionService;
import com.psicogest.psicogest.security.crypto.EncryptionContext;
import com.psicogest.psicogest.security.crypto.EncryptedEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    public MedicalRecordService(
            MedicalRecordRepository medicalRecordRepository,
            PatientRepository patientRepository,
            PsychoanalystRepository psychoanalystRepository,
            TherapeuticRelationshipRepository relationshipRepository,
            AppointmentRepository appointmentRepository,
            ClinicalEncryptionService encryptionService,
            AuditService auditService,
            MedicalRecordStateMachine stateMachine,
            ClinicalAccessDetector accessDetector
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
    }

    @Transactional
    public MedicalRecordResponseDTO create(
            Long patientId,
            MedicalRecordCreateDTO dto,
            SecurityActor actor
    ) {

        // 17. Resolver psicanalista
        Psychoanalyst psychoanalyst =
                psychoanalystRepository
                        .findByUserId(
                                actor.userId()
                        )
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Acesso negado"
                                        )
                        );

        // Paciente
        Patient patient =
                patientRepository
                        .findById( patientId )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Paciente não encontrado"
                                        )
                        );

        // 18. Exigir vínculo ACTIVE para escrever
        TherapeuticRelationship relationship =
                relationshipRepository
                        .findByPatientIdAndPsychoanalystIdAndStatus(
                                patientId,
                                psychoanalyst.getId(),
                                TherapeuticRelationshipStatus.ACTIVE
                        )
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Não existe vínculo terapêutico ativo com este paciente"
                                        )
                        );

        // 19. Resolver Appointment
        Appointment appointment = null;

        if (
                dto.appointmentId()
                        != null
        ) {

            appointment =
                    appointmentRepository
                            .findById(
                                    dto.appointmentId()
                            )
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Consulta não encontrada"
                                            )
                            );

            if (
                    !appointment.getPatient()
                            .getId()
                            .equals( patientId )
            ) {

                throw new MedicalRecordConflictException(
                        "A consulta não pertence ao paciente"
                );
            }

            if (
                    !appointment.getPsychoanalyst()
                            .getId()
                            .equals(
                                    psychoanalyst.getId()
                            )
            ) {

                throw new MedicalRecordConflictException(
                        "A consulta não pertence ao profissional autenticado"
                );
            }

            if (
                    medicalRecordRepository
                            .existsByAppointmentId(
                                    appointment.getId()
                            )
            ) {

                throw new MedicalRecordConflictException(
                        "Esta consulta já possui prontuário"
                );
            }
        }

        // 20. Gerar UUID antes da criptografia
        UUID recordId =
                UUID.randomUUID();

        // Contexto
        EncryptionContext encryptionContext =
                new EncryptionContext(
                        "MEDICAL_RECORD",
                        recordId.toString(),
                        patientId,
                        "content"
                );

        // 21. Criptografar
        EncryptedEnvelope encrypted =
                encryptionService.encrypt(
                        dto.content(),
                        encryptionContext
                );

        // 22. Criar entity
        Instant now =
                Instant.now();

        MedicalRecord record =
                MedicalRecord.builder()

                        .id( recordId )

                        .patient( patient )

                        .authorPsychoanalyst(
                                psychoanalyst
                        )

                        .therapeuticRelationship(
                                relationship
                        )

                        .appointment(
                                appointment
                        )

                        .status(
                                MedicalRecordStatus.DRAFT
                        )

                        .encryptedContent(
                                encrypted.ciphertext()
                        )

                        .contentIv(
                                encrypted.iv()
                        )

                        .encryptedDek(
                                encrypted.wrappedDataKey()
                        )

                        .cryptoVersion(
                                encrypted.cryptoVersion()
                        )

                        .cryptoAlgorithm(
                                encrypted.algorithm()
                        )

                        .keyId(
                                encrypted.keyId()
                        )

                        .version( 0L )

                        .createdAt( now )

                        .updatedAt( now )

                        .build();

        // 23. Persistir e auditar na MESMA transaction
        MedicalRecord saved =
                medicalRecordRepository
                        .saveAndFlush( record );

        auditService.recordCriticalWrite(
                new AuditCommand(
                        actor.userId(),
                        actor.sessionId(),
                        AuditAction
                                .MEDICAL_RECORD_CREATED,
                        "MEDICAL_RECORD",
                        saved.getId()
                                .toString(),
                        patientId,
                        null,
                        AuditOutcome.SUCCESS,
                        actor.correlationId(),
                        actor.sourceIp(),
                        actor.userAgentHash(),
                        Map.of(
                                "status",
                                saved.getStatus()
                                        .name(),
                                "version",
                                saved.getVersion()
                        )
                )
        );

        log.info(
                "Prontuário criado: recordId={}, patientId={}, psychoanalystId={}, correlationId={}",
                saved.getId(),
                patientId,
                psychoanalyst.getId(),
                actor.correlationId()
        );

        return toResponseDTO( saved );
    }

    private MedicalRecordResponseDTO toResponseDTO(
            MedicalRecord record
    ) {

        return new MedicalRecordResponseDTO(
                record.getId(),
                record.getPatient().getId(),
                record.getAuthorPsychoanalyst().getId(),
                record.getTherapeuticRelationship().getId(),
                record.getAppointment() != null
                        ? record.getAppointment().getId()
                        : null,
                record.getStatus(),
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
