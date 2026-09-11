package com.psicogest.psicogest.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psicogest.psicogest.model.entity.SubscriptionPlan;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {
}
