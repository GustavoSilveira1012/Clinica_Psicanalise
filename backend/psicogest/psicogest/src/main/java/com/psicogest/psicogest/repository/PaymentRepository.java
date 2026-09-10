package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Payment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(
            String idempotencyKey
    );

    Optional<Payment>
    findByProviderAndProviderTransactionId(
            String provider,
            String providerTransactionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.id = :paymentId
            """)
    Optional<Payment> findByIdForUpdate(
            @Param("paymentId")
            UUID paymentId
    );
}