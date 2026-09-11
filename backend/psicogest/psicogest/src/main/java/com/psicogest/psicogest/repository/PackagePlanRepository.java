package com.psicogest.psicogest.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psicogest.psicogest.model.entity.PackagePlan;

public interface PackagePlanRepository extends JpaRepository<PackagePlan, UUID> {
}
