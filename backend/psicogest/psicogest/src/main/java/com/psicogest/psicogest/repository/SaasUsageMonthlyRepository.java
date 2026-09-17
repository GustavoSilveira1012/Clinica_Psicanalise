package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.SaasUsageMonthly;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface SaasUsageMonthlyRepository extends JpaRepository<SaasUsageMonthly, UUID> {
    Optional<SaasUsageMonthly> findByOrganizationIdAndFeatureCodeAndPeriodStart(
            UUID organizationId, String featureCode, LocalDate periodStart);
}
