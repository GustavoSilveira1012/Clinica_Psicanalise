package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentAllocationRepository
        extends JpaRepository<
                PaymentAllocation,
                UUID
        > {

    @Query("""
            SELECT COALESCE(
                SUM(a.amount),
                0
            )

            FROM PaymentAllocation a

            WHERE a.payment.id = :paymentId
            """)
    BigDecimal sumAllocatedByPayment(
            @Param("paymentId")
            UUID paymentId
    );


    @Query("""
            SELECT COALESCE(
                SUM(a.amount),
                0
            )

            FROM PaymentAllocation a

            WHERE a.receivable.id = :receivableId

              AND a.payment.status IN (
                  com.psicogest.model.enums.PaymentStatus.CONFIRMED,
                  com.psicogest.model.enums.PaymentStatus.PARTIALLY_REFUNDED
              )
            """)
    BigDecimal sumEffectiveAllocation(
            @Param("receivableId")
            UUID receivableId
    );


    List<PaymentAllocation>
    findByPaymentId(
            UUID paymentId
    );


    List<PaymentAllocation>
    findByReceivableId(
            UUID receivableId
    );
}