package com.psicogest.psicogest.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psicogest.psicogest.model.entity.SubscriptionCycle;

import jakarta.persistence.LockModeType;

public interface SubscriptionCycleRepository extends JpaRepository<SubscriptionCycle, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SubscriptionCycle c where c.id = :id")
    Optional<SubscriptionCycle> findByIdForUpdate(@Param("id") UUID id);

    @Query("select coalesce(max(c.cycleNumber), 0) from SubscriptionCycle c where c.subscription.id = :subscriptionId")
    Integer nextCycleNumber(@Param("subscriptionId") UUID subscriptionId);

    Optional<SubscriptionCycle> findBySubscriptionIdAndPeriodStart(UUID subscriptionId, LocalDate periodStart);

    List<SubscriptionCycle> findBySubscriptionIdOrderByCycleNumberDesc(UUID subscriptionId);

    @Query("select c from SubscriptionCycle c where c.status = 'BILLED' and c.receivable.dueDate <= :today")
    List<SubscriptionCycle> findBilledDue(@Param("today") LocalDate today);
}
