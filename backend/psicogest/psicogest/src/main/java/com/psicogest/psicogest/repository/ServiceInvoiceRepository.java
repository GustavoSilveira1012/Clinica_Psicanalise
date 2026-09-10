package com.psicogest.psicogest.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.ServiceInvoice;

import jakarta.persistence.LockModeType;

@Repository
public interface ServiceInvoiceRepository
        extends JpaRepository<ServiceInvoice, UUID> {

    /**
     * Encontra invoice para atualização com lock pessimista
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT si
        FROM ServiceInvoice si
        WHERE si.id = :id
    """)
    Optional<ServiceInvoice> findByIdForUpdate(UUID id);

    /**
     * Soma dos valores de notas fiscais ativas
     * que já comprometem um Receivable
     * 
     * Status considerados:
     * - PENDING
     * - PROCESSING
     * - AUTHORIZED
     * - CANCEL_PENDING
     * - RECONCILIATION_REQUIRED
     */
    @Query("""
        SELECT COALESCE(SUM(si.netAmount), 0)
        FROM ServiceInvoice si
        WHERE si.clinic.id = :clinicId
          AND si.status IN (
              'PENDING',
              'PROCESSING',
              'AUTHORIZED',
              'CANCEL_PENDING',
              'RECONCILIATION_REQUIRED'
          )
    """)
    BigDecimal sumActiveFiscalOrigins(Long clinicId);
}
