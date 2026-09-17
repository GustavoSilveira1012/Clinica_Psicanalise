package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.OrganizationOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationOnboardingRepository extends JpaRepository<OrganizationOnboarding, UUID> {
    Optional<OrganizationOnboarding> findByOrganizationId(UUID organizationId);
}
