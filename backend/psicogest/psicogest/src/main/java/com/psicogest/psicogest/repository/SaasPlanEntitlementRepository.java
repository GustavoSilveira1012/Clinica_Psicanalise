package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.SaasPlanEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaasPlanEntitlementRepository extends JpaRepository<SaasPlanEntitlement, UUID> {
    List<SaasPlanEntitlement> findAllByPlanVersionId(UUID planVersionId);
    Optional<SaasPlanEntitlement> findByPlanVersionIdAndFeatureCode(UUID planVersionId, String featureCode);
}
