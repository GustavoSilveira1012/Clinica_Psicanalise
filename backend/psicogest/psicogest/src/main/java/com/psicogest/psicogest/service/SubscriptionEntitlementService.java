package com.psicogest.psicogest.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.model.entity.PackagePlanItem;
import com.psicogest.psicogest.model.entity.PackagePlanVersion;
import com.psicogest.psicogest.model.entity.PatientPackage;
import com.psicogest.psicogest.model.entity.SubscriptionCycle;
import com.psicogest.psicogest.model.enums.PatientPackageSource;
import com.psicogest.psicogest.model.enums.PatientPackageStatus;
import com.psicogest.psicogest.model.enums.SubscriptionCycleStatus;
import com.psicogest.psicogest.model.enums.SubscriptionGrantPolicy;
import com.psicogest.psicogest.repository.PackagePlanItemRepository;
import com.psicogest.psicogest.repository.PatientPackageRepository;
import com.psicogest.psicogest.repository.SubscriptionCycleRepository;

@Service
public class SubscriptionEntitlementService {

    public enum Trigger {
        CYCLE_START,
        FIRST_PAYMENT,
        FULL_PAYMENT
    }

    private final SubscriptionCycleRepository cycleRepository;
    private final PatientPackageRepository packageRepository;
    private final PackagePlanItemRepository itemRepository;
    private final SessionCreditService sessionCreditService;
    private final Clock clock;

    public SubscriptionEntitlementService(
            SubscriptionCycleRepository cycleRepository,
            PatientPackageRepository packageRepository,
            PackagePlanItemRepository itemRepository,
            SessionCreditService sessionCreditService,
            Clock clock
    ) {
        this.cycleRepository = cycleRepository;
        this.packageRepository = packageRepository;
        this.itemRepository = itemRepository;
        this.sessionCreditService = sessionCreditService;
        this.clock = clock;
    }

    @Transactional
    public void grantIfAllowed(UUID cycleId, Trigger trigger) {
        SubscriptionCycle cycle = cycleRepository.findByIdForUpdate(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Ciclo não encontrado"));
        SubscriptionGrantPolicy policy = cycle.getSubscription().getSubscriptionPlanVersion().getGrantPolicy();
        boolean allowed = switch (policy) {
            case ON_CYCLE_START -> trigger == Trigger.CYCLE_START;
            case ON_FIRST_PAYMENT -> trigger == Trigger.FIRST_PAYMENT || trigger == Trigger.FULL_PAYMENT;
            case ON_FULL_PAYMENT -> trigger == Trigger.FULL_PAYMENT;
        };
        if (allowed) {
            materialize(cycle);
        }
    }

    @Transactional
    public void grantCycleEntitlement(UUID cycleId) {
        SubscriptionCycle cycle = cycleRepository.findByIdForUpdate(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Ciclo não encontrado"));
        materialize(cycle);
    }

    private void materialize(SubscriptionCycle cycle) {
        if (cycle.getPatientPackage() != null || cycle.getStatus() == SubscriptionCycleStatus.ENTITLEMENT_GRANTED) {
            return;
        }
        if (cycle.getStatus() == SubscriptionCycleStatus.CANCELLED
                || cycle.getStatus() == SubscriptionCycleStatus.FAILED
                || cycle.getStatus() == SubscriptionCycleStatus.CLOSED) {
            throw new IllegalStateException("Ciclo terminal não pode receber entitlement automaticamente");
        }

        PackagePlanVersion packageVersion = cycle.getSubscription()
                .getSubscriptionPlanVersion()
                .getEntitlementPackageVersion();
        List<PackagePlanItem> items = itemRepository.findByPackagePlanVersionId(packageVersion.getId());
        if (items.isEmpty()) {
            throw new IllegalStateException("O pacote de entitlement não possui itens");
        }
        PackagePlanItem primaryItem = items.get(0);
        long quantity = items.stream().mapToLong(item -> item.getQuantity().longValue()).sum();
        Instant now = clock.instant();
        var subscription = cycle.getSubscription();
        var rollover = subscription.getSubscriptionPlanVersion().getRolloverPolicy();
        Instant expiresAt = rollover.name().equals("EXPIRE_AT_CYCLE_END")
                ? cycle.getPeriodEnd().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                : null;

        PatientPackage patientPackage = PatientPackage.builder()
                .id(UUID.randomUUID())
                .patient(subscription.getPatient())
                .financialEntityId(subscription.getFinancialEntityId())
                .packagePlanVersion(packageVersion)
                .status(PatientPackageStatus.PENDING_ACTIVATION)
                .purchasedAt(now)
                .startsAt(cycle.getPeriodStart().atStartOfDay().toInstant(ZoneOffset.UTC))
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .source(PatientPackageSource.SUBSCRIPTION_CYCLE)
                .purchaseAmount(cycle.getBilledAmount())
                .subscriptionCycleId(cycle.getId())
                .build();
        patientPackage.activate(now);
        PatientPackage saved = packageRepository.saveAndFlush(patientPackage);
        sessionCreditService.recordPackageActivation(
                subscription.getPatient(), saved, primaryItem.getId(), quantity, null);
        cycle.markEntitlementGranted(saved, now);
        cycleRepository.saveAndFlush(cycle);
    }
}
