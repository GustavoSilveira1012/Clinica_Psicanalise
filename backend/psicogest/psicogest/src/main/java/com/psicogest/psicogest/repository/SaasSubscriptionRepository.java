package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.SaasSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SaasSubscriptionRepository extends JpaRepository<SaasSubscription, UUID> {
    Optional<SaasSubscription> findByOrganizationId(UUID organizationId);
}
