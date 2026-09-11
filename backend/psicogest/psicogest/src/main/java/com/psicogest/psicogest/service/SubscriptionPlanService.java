package com.psicogest.psicogest.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.dto.CreateSubscriptionPlanRequest;
import com.psicogest.psicogest.dto.CreateSubscriptionPlanVersionRequest;
import com.psicogest.psicogest.model.entity.PackagePlanVersion;
import com.psicogest.psicogest.model.entity.SubscriptionPlan;
import com.psicogest.psicogest.model.entity.SubscriptionPlanVersion;
import com.psicogest.psicogest.model.enums.SubscriptionPlanVersionStatus;
import com.psicogest.psicogest.repository.PackagePlanVersionRepository;
import com.psicogest.psicogest.repository.SubscriptionPlanRepository;
import com.psicogest.psicogest.repository.SubscriptionPlanVersionRepository;

@Service
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionPlanVersionRepository versionRepository;
    private final PackagePlanVersionRepository packageVersionRepository;
    private final Clock clock;

    public SubscriptionPlanService(
            SubscriptionPlanRepository planRepository,
            SubscriptionPlanVersionRepository versionRepository,
            PackagePlanVersionRepository packageVersionRepository,
            Clock clock
    ) {
        this.planRepository = planRepository;
        this.versionRepository = versionRepository;
        this.packageVersionRepository = packageVersionRepository;
        this.clock = clock;
    }

    @Transactional
    public SubscriptionPlan createPlan(CreateSubscriptionPlanRequest request) {
        Instant now = clock.instant();
        return planRepository.save(SubscriptionPlan.builder()
                .id(UUID.randomUUID())
                .financialEntityId(request.financialEntityId())
                .name(request.name().trim())
                .description(request.description())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    @Transactional
    public SubscriptionPlanVersion createVersion(
            UUID planId,
            CreateSubscriptionPlanVersionRequest request
    ) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plano de assinatura não encontrado"));
        PackagePlanVersion packageVersion = packageVersionRepository.findById(request.entitlementPackageVersionId())
                .orElseThrow(() -> new IllegalArgumentException("Versão de pacote não encontrada"));
        if (!packageVersion.isPublished()) {
            throw new IllegalStateException("A versão do pacote precisa estar publicada");
        }

        int nextVersion = versionRepository.findAll().stream()
                .filter(v -> v.getSubscriptionPlan().getId().equals(planId))
                .mapToInt(SubscriptionPlanVersion::getVersion)
                .max()
                .orElse(0) + 1;

        return versionRepository.save(SubscriptionPlanVersion.builder()
                .id(UUID.randomUUID())
                .subscriptionPlan(plan)
                .version(nextVersion)
                .status(SubscriptionPlanVersionStatus.DRAFT)
                .entitlementPackageVersion(packageVersion)
                .cyclePrice(request.cyclePrice())
                .currency("BRL")
                .billingInterval(request.billingInterval())
                .intervalCount(request.intervalCount())
                .grantPolicy(request.grantPolicy())
                .rolloverPolicy(request.rolloverPolicy())
                .cancellationPolicy(request.cancellationPolicy())
                .graceDays(request.graceDays())
                .effectiveFrom(request.effectiveFrom())
                .createdAt(clock.instant())
                .build());
    }

    @Transactional
    public SubscriptionPlanVersion publishVersion(UUID versionId) {
        SubscriptionPlanVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Versão de assinatura não encontrada"));
        versionRepository.findAll().stream()
                .filter(other -> other.getSubscriptionPlan().getId().equals(version.getSubscriptionPlan().getId()))
                .filter(other -> other.getStatus() == SubscriptionPlanVersionStatus.PUBLISHED)
                .forEach(other -> {
                    other.setStatus(SubscriptionPlanVersionStatus.RETIRED);
                    other.setRetiredAt(clock.instant());
                    versionRepository.saveAndFlush(other);
                });
        version.publish(clock.instant());
        return versionRepository.saveAndFlush(version);
    }
}
