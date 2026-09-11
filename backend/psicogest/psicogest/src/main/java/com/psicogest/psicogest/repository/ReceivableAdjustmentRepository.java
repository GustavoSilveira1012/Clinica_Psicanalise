package com.psicogest.psicogest.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.ReceivableAdjustment;

/**
 * Repository para ReceivableAdjustment
 * 
 * Gerencia ajustes de cobrança (receivables)
 */
@Repository
public interface ReceivableAdjustmentRepository extends JpaRepository<ReceivableAdjustment, UUID> {

    /**
     * Calcula o saldo total de ajustes para uma cobrança
     * 
     * INCREASE: soma
     * DECREASE: subtrai
     * 
     * @param receivableId ID da cobrança
     * @return Saldo total de ajustes (INCREASE - DECREASE)
     */
    @Query("""
        SELECT COALESCE(
            SUM(
                CASE
                    WHEN a.direction = 
                        com.psicogest.psicogest.model.enums.ReceivableAdjustmentDirection.INCREASE
                    THEN a.amount
                    ELSE -a.amount
                END
            ),
            0
        )
        FROM ReceivableAdjustment a
        WHERE a.receivable.id = :receivableId
    """)
    BigDecimal calculateAdjustmentBalance(@Param("receivableId") UUID receivableId);
}
