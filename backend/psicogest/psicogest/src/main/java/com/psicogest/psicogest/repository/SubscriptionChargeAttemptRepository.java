package com.psicogest.psicogest.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psicogest.psicogest.model.entity.SubscriptionChargeAttempt;
import com.psicogest.psicogest.model.enums.SubscriptionChargeAttemptStatus;

import jakarta.persistence.LockModeType;

public interface SubscriptionChargeAttemptRepository extends JpaRepository<SubscriptionChargeAttempt, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SubscriptionChargeAttempt a where a.id = :id")
    Optional<SubscriptionChargeAttempt> findByIdForUpdate(@Param("id") UUID id);

    Optional<SubscriptionChargeAttempt> findByIdempotencyKey(String idempotencyKey);

    Optional<SubscriptionChargeAttempt> findTopBySubscriptionCycleIdOrderByAttemptNumberDesc(UUID cycleId);

    @Query("select a from SubscriptionChargeAttempt a where a.subscriptionCycle.id = :cycleId and a.status in :statuses order by a.attemptNumber desc")
    Optional<SubscriptionChargeAttempt> findOpenByCycleId(
            @Param("cycleId") UUID cycleId,
            @Param("statuses") java.util.Collection<SubscriptionChargeAttemptStatus> statuses);
}
