package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.saas.SaasPlanVersion;
import com.psicogest.psicogest.model.enums.SaasPlanVersionStatus;
import com.psicogest.psicogest.repository.SaasPlanEntitlementRepository;
import com.psicogest.psicogest.repository.SaasPlanVersionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class PublicPlanCatalogService {
    private final SaasPlanVersionRepository versions;
    private final SaasPlanEntitlementRepository entitlements;
    private final boolean plansEnabled;
    private final boolean pricingApproved;

    public PublicPlanCatalogService(
            SaasPlanVersionRepository versions,
            SaasPlanEntitlementRepository entitlements,
            @Value("${app.saas.public-plans-enabled:false}") boolean plansEnabled,
            @Value("${app.saas.public-pricing-approved:false}") boolean pricingApproved) {
        this.versions = versions;
        this.entitlements = entitlements;
        this.plansEnabled = plansEnabled;
        this.pricingApproved = pricingApproved;
    }

    @Transactional(readOnly = true)
    public PublicPlansResponse publicPlans() {
        if (!plansEnabled || !pricingApproved) {
            return new PublicPlansResponse(false, List.of());
        }

        var latestByCode = new LinkedHashMap<String, SaasPlanVersion>();
        for (SaasPlanVersion version : versions.findActiveCatalogVersions(SaasPlanVersionStatus.PUBLISHED)) {
            latestByCode.putIfAbsent(version.getPlan().getCode(), version);
        }

        List<PublicPlan> plans = latestByCode.values().stream().map(version -> {
            List<PublicFeature> features = entitlements.findAllByPlanVersionId(version.getId()).stream()
                    .filter(item -> Boolean.TRUE.equals(item.getEnabled())
                            && Boolean.TRUE.equals(item.getFeature().getActive()))
                    .map(item -> new PublicFeature(
                            item.getFeature().getCode(),
                            item.getFeature().getName(),
                            item.getLimitValue(),
                            item.getFeature().getUnit()))
                    .toList();
            return new PublicPlan(
                    version.getPlan().getCode(),
                    version.getPlan().getName(),
                    version.getPlan().getDescription(),
                    version.getMonthlyPrice(),
                    version.getCurrency(),
                    features);
        }).toList();

        return new PublicPlansResponse(!plans.isEmpty(), plans);
    }

    public record PublicPlansResponse(boolean available, List<PublicPlan> plans) {}

    public record PublicPlan(String code, String name, String description,
                             BigDecimal monthlyPrice, String currency,
                             List<PublicFeature> features) {}

    public record PublicFeature(String code, String name, Long limitValue, String unit) {}
}
