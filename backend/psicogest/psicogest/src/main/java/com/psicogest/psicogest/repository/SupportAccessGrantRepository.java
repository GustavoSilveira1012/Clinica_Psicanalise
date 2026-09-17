package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.SupportAccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupportAccessGrantRepository extends JpaRepository<SupportAccessGrant, UUID> {
    List<SupportAccessGrant> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
