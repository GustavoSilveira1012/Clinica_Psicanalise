package com.psicogest.psicogest.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.dto.PackagePlanCatalogResponse;
import com.psicogest.psicogest.model.entity.PackagePlan;
import com.psicogest.psicogest.model.entity.PackagePlanVersion;
import com.psicogest.psicogest.repository.PackagePlanRepository;
import com.psicogest.psicogest.repository.PackagePlanVersionRepository;
import com.psicogest.psicogest.repository.PatientSubscriptionRepository;

@Service
public class PackageCatalogService {

    private final PackagePlanRepository planRepository;
    private final PackagePlanVersionRepository versionRepository;
    private final PatientSubscriptionRepository subscriptionRepository;

    public PackageCatalogService(
            PackagePlanRepository planRepository,
            PackagePlanVersionRepository versionRepository,
            PatientSubscriptionRepository subscriptionRepository
    ) {
        this.planRepository = planRepository;
        this.versionRepository = versionRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public List<PackagePlanCatalogResponse> list() {
        return planRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private PackagePlanCatalogResponse toResponse(PackagePlan plan) {
        PackagePlanVersion version = versionRepository
                .findFirstByPackagePlanIdOrderByVersionDesc(plan.getId())
                .orElse(null);
        int sessions = version == null ? 0 : version.totalSessions();
        BigDecimal price = version == null || version.getTotalPrice() == null
                ? BigDecimal.ZERO
                : version.getTotalPrice();
        long activeSubscriptions = version == null
                ? 0
                : subscriptionRepository.countActiveByEntitlementPackageVersionId(version.getId());
        return new PackagePlanCatalogResponse(
                plan.getId(),
                plan.getName(),
                "ACTIVE".equalsIgnoreCase(plan.getStatus()) ? "ACTIVE" : "DRAFT",
                sessions,
                price,
                activeSubscriptions,
                plan.getUpdatedAt());
    }
}
