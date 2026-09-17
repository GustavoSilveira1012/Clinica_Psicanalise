package com.psicogest.psicogest.service.saas;

import com.psicogest.psicogest.repository.FeatureFlagRepository;
import com.psicogest.psicogest.security.tenant.TenantContextResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Map;

/** Flags control rollout; they never replace plan entitlements. */
@Service
public class FeatureFlagService {

    private final TenantContextResolver tenantContextResolver;
    private final FeatureFlagRepository repository;

    public FeatureFlagService(
            TenantContextResolver tenantContextResolver,
            FeatureFlagRepository repository
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(
            Authentication authentication,
            String featureKey,
            UUID organizationId
    ) {
        if (featureKey == null || !featureKey.matches("[A-Za-z0-9_.-]{1,100}")) {
            return false;
        }
        tenantContextResolver.resolve(authentication, organizationId);
        return repository.findByOrganizationIdAndFlagKey(organizationId, featureKey)
                .or(() -> repository.findByOrganizationIsNullAndFlagKey(featureKey))
                .map(flag -> isEnabledForOrganization(
                        Boolean.TRUE.equals(flag.getEnabled()),
                        flag.getRollout(),
                        organizationId,
                        featureKey))
                .orElse(false);
    }

    private boolean isEnabledForOrganization(
            boolean enabled,
            Map<String, Object> rollout,
            UUID organizationId,
            String featureKey
    ) {
        if (!enabled) {
            return false;
        }
        if (rollout == null || rollout.isEmpty() || !rollout.containsKey("percentage")) {
            return true;
        }
        Object rawPercentage = rollout.get("percentage");
        if (!(rawPercentage instanceof Number number)) {
            return false;
        }
        int percentage = Math.max(0, Math.min(100, number.intValue()));
        if (percentage == 0) {
            return false;
        }
        if (percentage == 100) {
            return true;
        }
        int bucket = Math.floorMod((organizationId + ":" + featureKey).hashCode(), 100);
        return bucket < percentage;
    }
}
