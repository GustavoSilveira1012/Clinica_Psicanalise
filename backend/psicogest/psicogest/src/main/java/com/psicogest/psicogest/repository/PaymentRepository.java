package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Payment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, UUID> {

    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.clinic.id = :clinicId
              AND p.status IN (
                    com.psicogest.psicogest.model.entity.Payment$PaymentStatus.CONFIRMED,
                    com.psicogest.psicogest.model.entity.Payment$PaymentStatus.PARTIALLY_REFUNDED
              )
              AND p.receivedAt IS NOT NULL
            ORDER BY p.receivedAt DESC
            """)
    List<Payment> findConfirmedByClinicId(@Param("clinicId") Long clinicId);

    List<Payment> findByClinicIdOrderByCreatedAtDesc(Long clinicId);

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

    /**
     * Busca pagamento por provider e provider transaction ID com lock pessimista
     * Essencial para confirmação via webhook
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.provider = :provider
            AND p.providerTransactionId = :providerTransactionId
            """)
    Optional<Payment> findByProviderAndProviderTransactionIdForUpdate(
            @Param("provider")
            String provider,
            @Param("providerTransactionId")
            String providerTransactionId
    );
}
