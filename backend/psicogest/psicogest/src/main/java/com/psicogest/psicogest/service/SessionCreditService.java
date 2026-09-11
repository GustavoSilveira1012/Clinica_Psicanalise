package com.psicogest.psicogest.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.model.entity.PatientPackage;
import com.psicogest.psicogest.model.entity.SessionCreditEntry;
import com.psicogest.psicogest.model.enums.SessionCreditDirection;
import com.psicogest.psicogest.model.enums.SessionCreditEntryType;
import com.psicogest.psicogest.repository.SessionCreditEntryRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;

import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de gerenciamento de créditos de sessão
 * 
 * Responsável por:
 * - Registrar adição/consumo de créditos
 * - Calcular saldo disponível
 * - Aplicar regras de expiração
 */
@Slf4j
@Service
@Transactional
public class SessionCreditService {

    private final SessionCreditEntryRepository entryRepository;

    private final AuditService auditService;

    private final Clock clock;

    public SessionCreditService(
            SessionCreditEntryRepository entryRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.entryRepository = entryRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Ativa pacote e adiciona créditos
     * 
     * Chamado quando PatientPackage muda para ACTIVE
     */
    public void recordPackageActivation(
            Patient patient,
            PatientPackage patientPackage,
            long sessionCount,
            SecurityActor actor
    ) {

        recordPackageActivation(patient, patientPackage, null, sessionCount, actor);
    }

    public void recordPackageActivation(
            Patient patient,
            PatientPackage patientPackage,
            UUID packageItemId,
            long sessionCount,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        SessionCreditEntry entry = SessionCreditEntry
                .builder()

                .id(UUID.randomUUID())

                .patient(patient)

                .patientPackage(patientPackage)

                .packageItemId(packageItemId)

                .entryType(
                        SessionCreditEntryType
                                .PACKAGE_ACTIVATION
                )

                .direction(SessionCreditDirection.CREDIT)

                .sessionCount(sessionCount)

                .createdAt(now)

                .build();

        entryRepository.save(entry);

        // Auditoria
        if (actor != null) {
            auditService.recordCriticalWrite(

                    new AuditCommand(

                            actor.userId(),

                            actor.sessionId(),

                            AuditAction
                                    .PACKAGE_SESSION_CREDIT_GRANTED,

                            "PATIENT_PACKAGE",

                            patientPackage.getId()
                                    .toString(),

                            patient.getId(),

                            null,

                            AuditOutcome.SUCCESS,

                            actor.correlationId(),

                            actor.sourceIp(),

                            actor.userAgentHash(),

                            Map.of(
                                    "sessionCount",
                                    sessionCount
                            )
                    )
            );
        }

        log.info(
                "Créditos ativados: patientId={}, packageId={}, count={}",
                patient.getId(),
                patientPackage.getId(),
                sessionCount
        );
    }

    /**
     * Consome créditos em agendamento
     */
    public void recordAppointmentConsumption(
            Patient patient,
            UUID appointmentId
    ) {

        Instant now = clock.instant();

        SessionCreditEntry entry = SessionCreditEntry
                .builder()

                .id(UUID.randomUUID())

                .patient(patient)

                .entryType(
                        SessionCreditEntryType
                                .APPOINTMENT_CONSUMPTION
                )

                .direction(SessionCreditDirection.DEBIT)

                .sessionCount(1L)

                .appointmentId(appointmentId)

                .createdAt(now)

                .build();

        entryRepository.save(entry);

        log.info(
                "Créditos consumidos: patientId={}, appointmentId={}",
                patient.getId(),
                appointmentId
        );
    }

    /**
     * Reverte consumo em cancelamento
     */
    public void recordConsumptionReversal(
            Patient patient,
            UUID appointmentId
    ) {

        Instant now = clock.instant();

        SessionCreditEntry entry = SessionCreditEntry
                .builder()

                .id(UUID.randomUUID())

                .patient(patient)

                .entryType(
                        SessionCreditEntryType
                                .CONSUMPTION_REVERSAL
                )

                .direction(SessionCreditDirection.CREDIT)

                .sessionCount(1L)

                .appointmentId(appointmentId)

                .createdAt(now)

                .build();

        entryRepository.save(entry);

        log.info(
                "Consumo revertido: patientId={}, appointmentId={}",
                patient.getId(),
                appointmentId
        );
    }

    /**
     * Registra expiração de pacote
     */
    public void recordPackageExpiration(
            Patient patient,
            PatientPackage patientPackage,
            long expiredCount
    ) {

        Instant now = clock.instant();

        SessionCreditEntry entry = SessionCreditEntry
                .builder()

                .id(UUID.randomUUID())

                .patient(patient)

                .patientPackage(patientPackage)

                .entryType(
                        SessionCreditEntryType.EXPIRATION
                )

                .direction(SessionCreditDirection.DEBIT)

                .sessionCount(expiredCount)

                .reason("Pacote expirou")

                .createdAt(now)

                .build();

        entryRepository.save(entry);

        log.info(
                "Pacote expirou: patientId={}, packageId={}, count={}",
                patient.getId(),
                patientPackage.getId(),
                expiredCount
        );
    }

    /**
     * Registra cancelamento de pacote
     */
    public void recordPackageCancellation(
            Patient patient,
            PatientPackage patientPackage,
            long cancelledCount,
            String reason
    ) {

        Instant now = clock.instant();

        SessionCreditEntry entry = SessionCreditEntry
                .builder()

                .id(UUID.randomUUID())

                .patient(patient)

                .patientPackage(patientPackage)

                .entryType(
                        SessionCreditEntryType
                                .PACKAGE_CANCELLATION
                )

                .direction(SessionCreditDirection.DEBIT)

                .sessionCount(cancelledCount)

                .reason(reason)

                .createdAt(now)

                .build();

        entryRepository.save(entry);

        log.info(
                "Pacote cancelado: patientId={}, packageId={}, count={}, reason={}",
                patient.getId(),
                patientPackage.getId(),
                cancelledCount,
                reason
        );
    }

    /**
     * Ajuste manual com auditoria
     * 
     * Requer permissão CREDIT_ADJUSTMENT
     */
    public void recordManualAdjustment(
            Patient patient,
            SessionCreditDirection direction,
            long sessionCount,
            String reason,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        SessionCreditEntry entry = SessionCreditEntry
                .builder()

                .id(UUID.randomUUID())

                .patient(patient)

                .entryType(
                        SessionCreditEntryType
                                .MANUAL_ADJUSTMENT
                )

                .direction(direction)

                .sessionCount(sessionCount)

                .reason(reason)

                .createdAt(now)

                .build();

        entryRepository.save(entry);

        // Auditoria com razão
        if (actor != null) {
            auditService.recordCriticalWrite(

                    new AuditCommand(

                            actor.userId(),

                            actor.sessionId(),

                            AuditAction
                                    .PACKAGE_CREDIT_MANUAL_ADJUSTMENT,

                            "SESSION_CREDIT_ENTRY",

                            entry.getId().toString(),

                            patient.getId(),

                            null,

                            AuditOutcome.SUCCESS,

                            actor.correlationId(),

                            actor.sourceIp(),

                            actor.userAgentHash(),

                            Map.of(
                                    "direction",
                                    direction.toString(),

                                    "sessionCount",
                                    sessionCount,

                                    "reason",
                                    reason
                            )
                    )
            );
        }

        log.info(
                "Ajuste manual: patientId={}, direction={}, count={}, reason={}, actor={}",
                patient.getId(),
                direction,
                sessionCount,
                reason,
                actor != null ? actor.userId() : "SYSTEM"
        );
    }

    /**
     * Calcula saldo total de créditos
     */
    public long getBalance(Long patientId) {
        return entryRepository
                .sumCreditsByPatient(patientId);
    }

    /**
     * Lista histórico de movimentos
     */
    public List<SessionCreditEntry> getHistory(
            Long patientId
    ) {
        return entryRepository
                .findAllByPatient(patientId);
    }

    /**
     * Lista movimentos de um pacote
     */
    public List<SessionCreditEntry> getPackageHistory(
            UUID packageId
    ) {
        return entryRepository
                .findAllByPackage(packageId);
    }
}
