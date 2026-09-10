package com.psicogest.psicogest.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.BankReconciliationAllocation;

/**
 * Repository para alocações de reconciliação bancária
 */
@Repository
public interface BankReconciliationAllocationRepository
        extends JpaRepository<BankReconciliationAllocation, UUID> {

    /**
     * Saldo conciliável de um lançamento bancário
     * 
     * BankTransaction.amount - SUM(allocations.amount)
     * Nunca pode ficar negativo.
     */
    @Query("""
            SELECT COALESCE(
                SUM(a.amount),
                0
            )
            FROM BankReconciliationAllocation a
            WHERE a.bankTransaction.id = :bankTransactionId
            """)
    BigDecimal sumReconciledAmount(
            UUID bankTransactionId
    );

    /**
     * Total já conciliado de um Payment
     * 
     * Payment.amount - SUM(allocations.amount where payment)
     */
    @Query("""
            SELECT COALESCE(
                SUM(a.amount),
                0
            )
            FROM BankReconciliationAllocation a
            WHERE a.payment.id = :paymentId
            """)
    BigDecimal sumReconciledForPayment(
            UUID paymentId
    );

    /**
     * Total já conciliado de um Refund
     */
    @Query("""
            SELECT COALESCE(
                SUM(a.amount),
                0
            )
            FROM BankReconciliationAllocation a
            WHERE a.refund.id = :refundId
            """)
    BigDecimal sumReconciledForRefund(
            UUID refundId
    );
}
