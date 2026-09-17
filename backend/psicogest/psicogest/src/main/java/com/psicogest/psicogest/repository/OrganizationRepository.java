package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    boolean existsBySlugIgnoreCase(String slug);
    Optional<Organization> findBySlugIgnoreCase(String slug);
}
