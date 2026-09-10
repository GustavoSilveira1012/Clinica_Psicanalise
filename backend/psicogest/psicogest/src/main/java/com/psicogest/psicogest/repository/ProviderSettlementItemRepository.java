package com.psicogest.psicogest.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.ProviderSettlementItem;

/**
 * Repository para itens de repasse
 */
@Repository
public interface ProviderSettlementItemRepository
        extends JpaRepository<ProviderSettlementItem, UUID> {

    /**
     * Calcula saldo líquido assinado de um repasse
     * 
     * Créditos - Débitos = resultado assinado
     * 
     * @param settlementId ID do repasse
     * @return valor assinado (positivo = crédito, negativo = débito)
     */
    @Query("""
            SELECT COALESCE(
                SUM(
                    CASE
                        WHEN i.direction =
                            com.psicogest.psicogest.model.enums.SettlementEntryDirection.CREDIT
                        THEN i.amount
                        ELSE -i.amount
                    END
                ),
                0
            )
            FROM ProviderSettlementItem i
            WHERE i.settlement.id = :settlementId
            """)
    BigDecimal calculateSignedNet(
            @Param("settlementId")
            UUID settlementId
    );

    /**
     * Total já liquidado de um Payment em repasses
     */
    @Query("""
            SELECT COALESCE(
                SUM(i.amount),
                0
            )
            FROM ProviderSettlementItem i
            WHERE i.payment.id = :paymentId
              AND i.itemType =
                com.psicogest.psicogest.model.enums.ProviderSettlementItemType.PAYMENT
            """)
    BigDecimal sumSettledForPayment(
            @Param("paymentId")
            UUID paymentId
    );

    /**
     * Total já liquidado de um Refund em repasses
     */
    @Query("""
            SELECT COALESCE(
                SUM(i.amount),
                0
            )
            FROM ProviderSettlementItem i
            WHERE i.refund.id = :refundId
              AND i.itemType =
                com.psicogest.psicogest.model.enums.ProviderSettlementItemType.REFUND
            """)
    BigDecimal sumSettledForRefund(
            @Param("refundId")
            UUID refundId
    );

    /**
     * Total de taxas de um repasse
     */
    @Query("""
            SELECT COALESCE(
                SUM(i.amount),
                0
            )
            FROM ProviderSettlementItem i
            WHERE i.settlement.id = :settlementId
              AND i.itemType =
                com.psicogest.psicogest.model.enums.ProviderSettlementItemType.PROVIDER_FEE
            """)
    BigDecimal sumProviderFees(
            @Param("settlementId")
            UUID settlementId
    );

    /**
     * Total de chargebacks de um repasse
     */
    @Query("""
            SELECT COALESCE(
                SUM(i.amount),
                0
            )
            FROM ProviderSettlementItem i
            WHERE i.settlement.id = :settlementId
              AND i.itemType =
                com.psicogest.psicogest.model.enums.ProviderSettlementItemType.CHARGEBACK
            """)
    BigDecimal sumChargebacks(
            @Param("settlementId")
            UUID settlementId
    );
}
