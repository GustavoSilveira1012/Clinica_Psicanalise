package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.CreditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Repository para entradas de crédito
 */
@Repository
public interface CreditEntryRepository
        extends JpaRepository<CreditEntry, UUID> {

    /**
     * 11. Calcula saldo da conta de crédito
     * Saldo = CREDIT - DEBIT (nunca persistido)
     */
    @Query("""
            SELECT COALESCE(
                SUM(
                    CASE
                        WHEN e.direction =
                            com.psicogest.psicogest.model.enums.CreditEntryDirection.CREDIT
                        THEN e.amount

                        ELSE -e.amount
                    END
                ),
                0
            )

            FROM CreditEntry e

            WHERE e.creditAccount.id = :accountId
            """)
    BigDecimal calculateBalance(
            @Param("accountId")
            UUID accountId
    );

    /**
     * 13. Soma créditos originados do cancelamento de uma cobrança
     */
    @Query("""
            SELECT COALESCE(
                SUM(e.amount),
                0
            )

            FROM CreditEntry e

            WHERE e.direction =
                com.psicogest.psicogest.model.enums.CreditEntryDirection.CREDIT

              AND e.entryType =
                com.psicogest.psicogest.model.enums.CreditEntryType.RECEIVABLE_CANCELLATION

              AND e.sourceReceivable.id =
                :receivableId
            """)
    BigDecimal sumCancellationCredits(
            @Param("receivableId")
            UUID receivableId
    );

    /**
     * 14. Soma créditos aplicados em uma cobrança
     */
    @Query("""
            SELECT COALESCE(
                SUM(e.amount),
                0
            )

            FROM CreditEntry e

            WHERE e.direction =
                com.psicogest.psicogest.model.enums.CreditEntryDirection.DEBIT

              AND e.entryType =
                com.psicogest.psicogest.model.enums.CreditEntryType.RECEIVABLE_APPLICATION

              AND e.targetReceivable.id =
                :receivableId
            """)
    BigDecimal sumAppliedCredits(
            @Param("receivableId")
            UUID receivableId
    );

    /**
     * 20. Soma créditos gerados de uma alocação específica
     * (para calcular quanto da alocação virou crédito)
     */
    @Query("""
            SELECT COALESCE(
                SUM(e.amount),
                0
            )

            FROM CreditEntry e

            WHERE e.direction =
                com.psicogest.psicogest.model.enums.CreditEntryDirection.CREDIT

              AND e.entryType =
                com.psicogest.psicogest.model.enums.CreditEntryType.RECEIVABLE_CANCELLATION

              AND e.sourcePaymentAllocation.id =
                :allocationId
            """)
    BigDecimal sumCreditCreatedFromAllocation(
            @Param("allocationId")
            UUID allocationId
    );
}
