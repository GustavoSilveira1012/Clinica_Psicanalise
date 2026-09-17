package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

    Optional<FeatureFlag> findByOrganizationIdAndFlagKey(UUID organizationId, String flagKey);

    Optional<FeatureFlag> findByOrganizationIsNullAndFlagKey(String flagKey);
}
