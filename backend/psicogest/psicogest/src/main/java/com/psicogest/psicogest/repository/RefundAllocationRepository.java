package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.RefundAllocation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para alocações de reembolso
 */
@Repository
public interface RefundAllocationRepository 
        extends JpaRepository<RefundAllocation, UUID> {

    /**
     * 16. Soma refunds de um payment específico
     */
    @Query("""
            SELECT COALESCE(SUM(ra.amount), 0)
            FROM RefundAllocation ra
            WHERE ra.paymentAllocation.payment.id = :paymentId
              AND ra.refund.status = 
                com.psicogest.psicogest.model.entity.Refund$RefundStatus.CONFIRMED
            """)
    BigDecimal sumConfirmedReversedByPayment(
            @Param("paymentId")
            UUID paymentId
    );

    /**
     * 18. Soma refunds de um receivable específico
     */
    @Query("""
            SELECT COALESCE(SUM(ra.amount), 0)
            FROM RefundAllocation ra
            WHERE ra.paymentAllocation.receivable.id = :receivableId
              AND ra.refund.status = 
                com.psicogest.psicogest.model.entity.Refund$RefundStatus.CONFIRMED
            """)
    BigDecimal sumRefundedForReceivable(
            @Param("receivableId")
            UUID receivableId
    );

    /**
     * 9. Soma valor reembolsado de uma allocation específica
     */
    @Query("""
            SELECT COALESCE(SUM(ra.amount), 0)
            FROM RefundAllocation ra
            WHERE ra.paymentAllocation.id = :allocationId
              AND ra.refund.status =
                com.psicogest.psicogest.model.entity.Refund$RefundStatus.CONFIRMED
            """)
    BigDecimal sumConfirmedRefundedAmount(
            @Param("allocationId")
            UUID allocationId
    );

    /**
     * 37. Busca todas as reversões de um refund
     */
    @Query("""
            SELECT ra
            FROM RefundAllocation ra
            WHERE ra.refund.id = :refundId
            ORDER BY ra.paymentAllocation.id
            """)
    java.util.List<RefundAllocation> findByRefundId(
            @Param("refundId")
            UUID refundId
    );
}
