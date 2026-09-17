package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.OrganizationMembership;
import com.psicogest.psicogest.model.enums.OrganizationMembershipStatus;
import com.psicogest.psicogest.model.enums.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

    Optional<OrganizationMembership> findByOrganizationIdAndUserIdAndStatus(
            UUID organizationId, Long userId, OrganizationMembershipStatus status);

    List<OrganizationMembership> findAllByUserIdAndStatusOrderByCreatedAtAsc(
            Long userId, OrganizationMembershipStatus status);

    List<OrganizationMembership> findAllByOrganizationIdAndStatusOrderByCreatedAtAsc(
            UUID organizationId, OrganizationMembershipStatus status);

    long countByOrganizationIdAndRoleAndStatus(
            UUID organizationId, OrganizationRole role, OrganizationMembershipStatus status);

    @Query("""
            select m from OrganizationMembership m
            join fetch m.user
            where m.organization.id = :organizationId
              and m.status = :status
            order by m.createdAt asc
            """)
    List<OrganizationMembership> findMembers(
            @Param("organizationId") UUID organizationId,
            @Param("status") OrganizationMembershipStatus status);
}
