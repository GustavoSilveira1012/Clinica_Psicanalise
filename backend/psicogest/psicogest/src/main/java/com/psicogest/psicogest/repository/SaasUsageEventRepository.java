package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.SaasUsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SaasUsageEventRepository extends JpaRepository<SaasUsageEvent, UUID> {

    Optional<SaasUsageEvent> findByOrganizationIdAndIdempotencyKey(
            UUID organizationId,
            String idempotencyKey
    );
}
