package com.psicogest.psicogest.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.dto.CancelSubscriptionRequest;
import com.psicogest.psicogest.dto.CreateSubscriptionRequest;
import com.psicogest.psicogest.dto.ResumeSubscriptionRequest;
import com.psicogest.psicogest.dto.SubscriptionResponse;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.model.entity.PatientSubscription;
import com.psicogest.psicogest.model.entity.SubscriptionPlanVersion;
import com.psicogest.psicogest.model.enums.SubscriptionCancellationPolicy;
import com.psicogest.psicogest.repository.PatientRepository;
import com.psicogest.psicogest.repository.PatientSubscriptionRepository;
import com.psicogest.psicogest.repository.SubscriptionPlanVersionRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;

@Service
public class SubscriptionService {
    private final PatientRepository patientRepository;
    private final SubscriptionPlanVersionRepository versionRepository;
    private final PatientSubscriptionRepository subscriptionRepository;
    private final AuditService auditService;
    private final Clock clock;

    public SubscriptionService(PatientRepository patientRepository,
            SubscriptionPlanVersionRepository versionRepository,
            PatientSubscriptionRepository subscriptionRepository,
            AuditService auditService, Clock clock) {
        this.patientRepository = patientRepository;
        this.versionRepository = versionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public SubscriptionResponse create(Long patientId, CreateSubscriptionRequest request, SecurityActor actor) {
        SubscriptionPlanVersion version = versionRepository.findPublishedById(request.subscriptionPlanVersionId())
                .orElseThrow(() -> new IllegalStateException("Versão publicada não encontrada"));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
        LocalDate today = LocalDate.now(clock);
        if (request.startsOn().isBefore(today)) {
            throw new IllegalArgumentException("A data de início não pode ser retroativa");
        }
        PatientSubscription saved = subscriptionRepository.saveAndFlush(PatientSubscription.create(
                UUID.randomUUID(), patient, version.getSubscriptionPlan().getFinancialEntityId(),
                version, request.startsOn(), clock.instant()));
        audit(actor, AuditAction.SUBSCRIPTION_CREATED, saved.getId(), patientId,
                Map.of("subscriptionPlanVersionId", version.getId().toString(), "startsOn", request.startsOn().toString()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse find(UUID id) {
        return toResponse(subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada")));
    }

    @Transactional
    public SubscriptionResponse pause(UUID id, SecurityActor actor) {
        PatientSubscription subscription = lock(id);
        subscription.pause(clock.instant());
        subscriptionRepository.saveAndFlush(subscription);
        audit(actor, AuditAction.SUBSCRIPTION_PAUSED, id, subscription.getPatient().getId(), Map.of());
        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse resume(UUID id, ResumeSubscriptionRequest request, SecurityActor actor) {
        PatientSubscription subscription = lock(id);
        if (request.resumeDate().isBefore(LocalDate.now(clock))) {
            throw new IllegalArgumentException("A retomada não pode ser retroativa");
        }
        subscription.resume(request.resumeDate(), clock.instant());
        subscriptionRepository.saveAndFlush(subscription);
        audit(actor, AuditAction.SUBSCRIPTION_RESUMED, id, subscription.getPatient().getId(),
                Map.of("resumeDate", request.resumeDate().toString()));
        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse cancel(UUID id, CancelSubscriptionRequest request, SecurityActor actor) {
        PatientSubscription subscription = lock(id);
        Instant now = clock.instant();
        if (request.atPeriodEnd()
                || subscription.getSubscriptionPlanVersion().getCancellationPolicy() == SubscriptionCancellationPolicy.END_OF_CURRENT_PERIOD) {
            subscription.requestCancellationAtPeriodEnd(now);
            audit(actor, AuditAction.SUBSCRIPTION_CANCELLATION_REQUESTED, id, subscription.getPatient().getId(),
                    Map.of("reason", request.reason()));
        } else {
            subscription.cancel(now);
            audit(actor, AuditAction.SUBSCRIPTION_CANCELLED, id, subscription.getPatient().getId(),
                    Map.of("reason", request.reason()));
        }
        subscriptionRepository.saveAndFlush(subscription);
        return toResponse(subscription);
    }

    private PatientSubscription lock(UUID id) {
        return subscriptionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada"));
    }

    private SubscriptionResponse toResponse(PatientSubscription s) {
        return new SubscriptionResponse(s.getId(), s.getPatient().getId(), s.getSubscriptionPlanVersion().getId(),
                s.getStatus(), s.getStartsOn(), s.getCurrentPeriodStart(), s.getCurrentPeriodEnd(),
                s.getNextCycleStart(), s.getBillingAnchorDay(), s.isCancelAtPeriodEnd(), s.getCreatedAt());
    }

    private void audit(SecurityActor actor, AuditAction action, UUID id, Long patientId, Map<String, Object> metadata) {
        if (actor == null || auditService == null) return;
        auditService.recordCriticalWrite(new AuditCommand(actor.userId(), actor.sessionId(), action,
                "PATIENT_SUBSCRIPTION", id.toString(), patientId, null, AuditOutcome.SUCCESS,
                actor.correlationId(), actor.sourceIp(), actor.userAgentHash(), metadata));
    }
}
