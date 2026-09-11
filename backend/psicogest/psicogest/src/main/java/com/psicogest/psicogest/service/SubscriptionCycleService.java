package com.psicogest.psicogest.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.model.entity.PatientSubscription;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.model.entity.SubscriptionCycle;
import com.psicogest.psicogest.model.enums.ReceivableOriginType;
import com.psicogest.psicogest.model.enums.SubscriptionCycleStatus;
import com.psicogest.psicogest.model.enums.SubscriptionStatus;
import com.psicogest.psicogest.repository.PatientSubscriptionRepository;
import com.psicogest.psicogest.repository.ReceivableRepository;
import com.psicogest.psicogest.repository.SubscriptionCycleRepository;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;

@Service
public class SubscriptionCycleService {

    private final PatientSubscriptionRepository subscriptionRepository;
    private final SubscriptionCycleRepository cycleRepository;
    private final ReceivableRepository receivableRepository;
    private final MonthlyBillingPeriodCalculator periodCalculator;
    private final SubscriptionEntitlementService entitlementService;
    private final AuditService auditService;
    private final Clock clock;

    public SubscriptionCycleService(
            PatientSubscriptionRepository subscriptionRepository,
            SubscriptionCycleRepository cycleRepository,
            ReceivableRepository receivableRepository,
            MonthlyBillingPeriodCalculator periodCalculator,
            SubscriptionEntitlementService entitlementService,
            AuditService auditService,
            Clock clock
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.cycleRepository = cycleRepository;
        this.receivableRepository = receivableRepository;
        this.periodCalculator = periodCalculator;
        this.entitlementService = entitlementService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public SubscriptionCycle createNextCycle(UUID subscriptionId) {
        PatientSubscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada"));
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);

        if (subscription.getStatus() == SubscriptionStatus.PENDING_START
                && !subscription.getStartsOn().isAfter(today)) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE
                && subscription.getStatus() != SubscriptionStatus.PAST_DUE) {
            throw new IllegalStateException("Assinatura não permite novo ciclo");
        }

        LocalDate start = subscription.getNextCycleStart();
        if (start == null) {
            start = subscription.getStartsOn();
        }
        if (subscription.isCancelAtPeriodEnd()
                && subscription.getCurrentPeriodEnd() != null
                && start.isAfter(subscription.getCurrentPeriodEnd())) {
            subscription.cancel(now);
            subscriptionRepository.saveAndFlush(subscription);
            return null;
        }

        if (cycleRepository.findBySubscriptionIdAndPeriodStart(subscriptionId, start).isPresent()) {
            return cycleRepository.findBySubscriptionIdAndPeriodStart(subscriptionId, start).orElseThrow();
        }

        BillingPeriod period = periodCalculator.calculate(start, subscription.getBillingAnchorDay());
        int cycleNumber = cycleRepository.nextCycleNumber(subscriptionId) + 1;
        UUID cycleId = UUID.randomUUID();
        BigDecimal amount = subscription.getSubscriptionPlanVersion().getCyclePrice();
        Instant billedAt = now;

        SubscriptionCycle cycle = SubscriptionCycle.builder()
                .id(cycleId)
                .subscription(subscription)
                .cycleNumber(cycleNumber)
                .periodStart(period.start())
                .periodEnd(period.end())
                .billingDate(period.start())
                .status(SubscriptionCycleStatus.BILLED)
                .billedAmount(amount)
                .currency(subscription.getSubscriptionPlanVersion().getCurrency())
                .billedAt(billedAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Receivable receivable = Receivable.builder()
                .id(UUID.randomUUID())
                .patient(subscription.getPatient())
                .description("Assinatura " + subscription.getSubscriptionPlanVersion().getSubscriptionPlan().getName()
                        + " - ciclo " + cycleNumber)
                .grossAmount(amount)
                .discountAmount(BigDecimal.ZERO)
                .netAmount(amount)
                .currency(subscription.getSubscriptionPlanVersion().getCurrency())
                .dueDate(period.start())
                .status(Receivable.ReceivableStatus.OPEN)
                .originType(ReceivableOriginType.SUBSCRIPTION_CYCLE)
                .originId(cycleId)
                .createdAt(now)
                .updatedAt(now)
                .build();
        receivableRepository.saveAndFlush(receivable);
        cycle.setReceivable(receivable);
        cycleRepository.saveAndFlush(cycle);

        subscription.advancePeriod(period.start(), period.end(), period.nextStart(), now);
        subscriptionRepository.saveAndFlush(subscription);
        audit(AuditAction.SUBSCRIPTION_CYCLE_CREATED, cycleId, subscription.getPatient().getId(),
                Map.of("cycleNumber", cycleNumber, "periodStart", period.start().toString(), "periodEnd", period.end().toString()));
        audit(AuditAction.SUBSCRIPTION_RECEIVABLE_CREATED, receivable.getId(), subscription.getPatient().getId(),
                Map.of("cycleId", cycleId.toString(), "amount", amount.toPlainString()));

        if (subscription.getSubscriptionPlanVersion().getGrantPolicy().name().equals("ON_CYCLE_START")) {
            entitlementService.grantIfAllowed(cycleId, SubscriptionEntitlementService.Trigger.CYCLE_START);
        }
        return cycle;
    }

    private void audit(AuditAction action, UUID resourceId, Long patientId, Map<String, Object> metadata) {
        if (auditService == null) {
            return;
        }
        auditService.recordCriticalWrite(new AuditCommand(
                null, null, action, "SUBSCRIPTION_CYCLE", resourceId.toString(), patientId,
                null, AuditOutcome.SUCCESS, null, null, null, metadata));
    }
}
