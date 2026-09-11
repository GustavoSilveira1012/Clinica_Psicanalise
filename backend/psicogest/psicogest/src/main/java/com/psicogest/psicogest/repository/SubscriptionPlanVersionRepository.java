package com.psicogest.psicogest.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psicogest.psicogest.model.entity.SubscriptionPlanVersion;

public interface SubscriptionPlanVersionRepository extends JpaRepository<SubscriptionPlanVersion, UUID> {

    @Query("select v from SubscriptionPlanVersion v where v.id = :id and v.status = 'PUBLISHED'")
    Optional<SubscriptionPlanVersion> findPublishedById(@Param("id") UUID id);
}
