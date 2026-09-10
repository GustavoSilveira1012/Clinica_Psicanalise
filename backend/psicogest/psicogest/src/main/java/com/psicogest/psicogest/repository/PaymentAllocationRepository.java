package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.PaymentAllocation;
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

@Repository
public interface PaymentAllocationRepository
        extends JpaRepository<PaymentAllocation, UUID> {

    @Query("""
            SELECT COALESCE(SUM(a.amount), 0)
            FROM PaymentAllocation a
            WHERE a.payment.id = :paymentId
            """)
    BigDecimal sumGrossAllocatedByPayment(
            @Param("paymentId")
            UUID paymentId
    );

    /**
     * Legacy: alias para sumGrossAllocatedByPayment
     */
    default BigDecimal sumAllocatedByPayment(UUID paymentId) {
        return sumGrossAllocatedByPayment(paymentId);
    }

    /**
     * 16. Alocações para receivable (considerando status de payment)
     */
    @Query("""
            SELECT COALESCE(SUM(a.amount), 0)
            FROM PaymentAllocation a
            WHERE a.receivable.id = :receivableId
              AND a.payment.status IN (
                com.psicogest.psicogest.model.entity.Payment$PaymentStatus.CONFIRMED,
                com.psicogest.psicogest.model.entity.Payment$PaymentStatus.PARTIALLY_REFUNDED,
                com.psicogest.psicogest.model.entity.Payment$PaymentStatus.REFUNDED
              )
            """)
    BigDecimal sumGrossForReceivable(
            @Param("receivableId")
            UUID receivableId
    );

    /**
     * Legacy: alias para sumGrossForReceivable
     */
    default BigDecimal sumEffectiveAllocation(UUID receivableId) {
        return sumGrossForReceivable(receivableId);
    }

    List<PaymentAllocation> findByPaymentId(UUID paymentId);

    List<PaymentAllocation> findByReceivableId(UUID receivableId);

    /**
     * 19. Busca allocations ordenadas por data/id
     * Para cancelamento com crédito
     */
    @Query("""
            SELECT a
            FROM PaymentAllocation a
            WHERE a.receivable.id = :receivableId
            ORDER BY a.createdAt, a.id
            """)
    List<PaymentAllocation> findByReceivableIdOrdered(
            @Param("receivableId")
            UUID receivableId
    );

    /**
     * 9. Busca allocation com lock pessimista
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM PaymentAllocation a
            WHERE a.id = :allocationId
            """)
    Optional<PaymentAllocation> findByIdForUpdate(
            @Param("allocationId")
            UUID allocationId
    );
}
