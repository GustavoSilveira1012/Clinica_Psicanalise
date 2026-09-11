package com.psicogest.psicogest.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psicogest.psicogest.model.entity.SubscriptionPaymentMandate;

public interface SubscriptionPaymentMandateRepository extends JpaRepository<SubscriptionPaymentMandate, UUID> {

    @Query("select m from SubscriptionPaymentMandate m where m.subscription.id = :subscriptionId and m.status = 'ACTIVE'")
    Optional<SubscriptionPaymentMandate> findActiveBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);
}
