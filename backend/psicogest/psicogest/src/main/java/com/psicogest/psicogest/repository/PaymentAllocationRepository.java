package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Repository para alocações de pagamento
 */
@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, UUID> {

    /**
     * 27. Calcula valor total alocado para uma conta
     * 
     * Soma allocations de pagamentos confirmados
     * Futuro: refunds serão considerados aqui também
     */
    @Query("""
            SELECT COALESCE(
                SUM(a.amount),
                0
            )
            FROM PaymentAllocation a
            WHERE a.receivable.id = :receivableId
              AND a.payment.status IN (
                com.psicogest.psicogest.model.entity.Payment$PaymentStatus.CONFIRMED,
                com.psicogest.psicogest.model.entity.Payment$PaymentStatus.PARTIALLY_REFUNDED
              )
            """)
    BigDecimal sumEffectiveAllocation(
            @Param("receivableId") UUID receivableId
    );
}
