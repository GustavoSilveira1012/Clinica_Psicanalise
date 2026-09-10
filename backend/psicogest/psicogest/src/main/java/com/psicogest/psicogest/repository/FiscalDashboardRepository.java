package com.psicogest.psicogest.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.ServiceInvoice;

/**
 * Queries analíticas para dashboard fiscal
 */
@Repository
public interface FiscalDashboardRepository
        extends JpaRepository<ServiceInvoice, java.util.UUID> {

    /**
     * Total de notas fiscais autorizadas
     */
    @Query("""
        SELECT COUNT(si)
        FROM ServiceInvoice si
        WHERE si.clinic.id = :clinicId
          AND si.status = 'AUTHORIZED'
    """)
    Long countAuthorized(Long clinicId);

    /**
     * Total de notas fiscais rejeitadas
     */
    @Query("""
        SELECT COUNT(si)
        FROM ServiceInvoice si
        WHERE si.clinic.id = :clinicId
          AND si.status = 'REJECTED'
    """)
    Long countRejected(Long clinicId);

    /**
     * Soma de valores líquidos autorizados
     */
    @Query("""
        SELECT COALESCE(SUM(si.netAmount), 0)
        FROM ServiceInvoice si
        WHERE si.clinic.id = :clinicId
          AND si.status = 'AUTHORIZED'
    """)
    BigDecimal sumAuthorizedAmount(Long clinicId);

    /**
     * Soma de valores brutos em período
     */
    @Query("""
        SELECT COALESCE(SUM(si.grossAmount), 0)
        FROM ServiceInvoice si
        WHERE si.clinic.id = :clinicId
          AND si.status = 'AUTHORIZED'
          AND CAST(si.authorizedAt AS DATE) BETWEEN :startDate AND :endDate
    """)
    BigDecimal sumGrossAmountInPeriod(
            Long clinicId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Soma de deduções em período
     */
    @Query("""
        SELECT COALESCE(SUM(si.deductions), 0)
        FROM ServiceInvoice si
        WHERE si.clinic.id = :clinicId
          AND si.status = 'AUTHORIZED'
          AND CAST(si.authorizedAt AS DATE) BETWEEN :startDate AND :endDate
    """)
    BigDecimal sumDeductionsInPeriod(
            Long clinicId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Taxa de rejeição (%)
     */
    @Query(value = """
        SELECT ROUND(
            COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) * 100.0 /
            COUNT(*),
            2
        )
        FROM service_invoices
        WHERE clinic_id = :clinicId
          AND status IN ('AUTHORIZED', 'REJECTED')
    """, nativeQuery = true)
    BigDecimal rejectionRate(Long clinicId);
}
