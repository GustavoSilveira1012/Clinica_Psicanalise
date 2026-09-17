package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.OrganizationInvite;
import com.psicogest.psicogest.model.enums.OrganizationInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationInviteRepository extends JpaRepository<OrganizationInvite, UUID> {
    Optional<OrganizationInvite> findByTokenHash(String tokenHash);
    boolean existsByOrganizationIdAndEmailHashAndStatus(
            UUID organizationId, String emailHash, OrganizationInviteStatus status);
}
