package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Refund;
import com.psicogest.psicogest.model.entity.Refund.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 8. Repository para reembolsos
 */
@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    /**
     * Busca refund por chave de idempotência
     */
    Optional<Refund> findByIdempotencyKey(
            String idempotencyKey
    );

    /**
     * Verifica se existe refund confirmado para pagamento
     */
    boolean existsByPaymentIdAndStatus(
            UUID paymentId,
            RefundStatus status
    );

    /**
     * Busca ID do pagamento associado ao refund
     */
    @Query("""
            SELECT r.payment.id
            FROM Refund r
            WHERE r.id = :refundId
            """)
    Optional<UUID> findPaymentId(
            @Param("refundId")
            UUID refundId
    );

    /**
     * 8. Busca refund com lock pessimista
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM Refund r
            WHERE r.id = :refundId
            """)
    Optional<Refund> findByIdForUpdate(
            @Param("refundId")
            UUID refundId
    );

    /**
     * 8. Soma reembolsos confirmados de um pagamento
     */
    @Query("""
            SELECT COALESCE(
                SUM(r.amount),
                0
            )

            FROM Refund r

            WHERE r.payment.id = :paymentId

            AND r.status =
                com.psicogest.psicogest.model.entity.Refund$RefundStatus.CONFIRMED
            """)
    BigDecimal sumConfirmedRefunds(
            @Param("paymentId")
            UUID paymentId
    );

    /**
     * Busca refund por provider e provider refund ID com lock pessimista
     * Essencial para confirmação de refund via webhook
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM Refund r
            WHERE r.provider = :provider
            AND r.providerRefundId = :providerRefundId
            """)
    Optional<Refund> findByProviderAndProviderRefundIdForUpdate(
            @Param("provider")
            String provider,
            @Param("providerRefundId")
            String providerRefundId
    );

    /**
     * Busca refunds de um cancelamento de pacote
     */
    @Query("""
            SELECT r
            FROM Refund r
            WHERE r.patientPackageCancellation.id = :cancellationId
            """)
    List<Refund> findByPatientPackageCancellationId(
            @Param("cancellationId")
            UUID cancellationId
    );
}
