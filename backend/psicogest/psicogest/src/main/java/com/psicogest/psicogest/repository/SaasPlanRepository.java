package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.SaasPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SaasPlanRepository extends JpaRepository<SaasPlan, UUID> {
    Optional<SaasPlan> findByCodeAndActiveTrue(String code);
}
