package com.psicogest.psicogest.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.psicogest.psicogest.model.entity.SessionCreditEntry;
import com.psicogest.psicogest.model.enums.SessionCreditEntryType;

@Repository
public interface SessionCreditEntryRepository
        extends JpaRepository<SessionCreditEntry, UUID> {

    /**
     * Lista todos os movimentos de um paciente
     * Ordenado por data descendente (mais recentes primeiro)
     */
    @Query("""
        SELECT e
        FROM SessionCreditEntry e
        WHERE e.patient.id = :patientId
        ORDER BY e.createdAt DESC
    """)
    List<SessionCreditEntry> findAllByPatient(
        @Param("patientId")
        Long patientId
    );

    /**
     * Lista movimentos de um pacote específico
     */
    @Query("""
        SELECT e
        FROM SessionCreditEntry e
        WHERE e.patientPackage.id = :packageId
        ORDER BY e.createdAt DESC
    """)
    List<SessionCreditEntry> findAllByPackage(
        @Param("packageId")
        UUID packageId
    );

    /**
     * Soma os créditos de um paciente
     * CREDIT = +, DEBIT = -
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN e.direction = 'CREDIT' THEN e.sessionCount
                WHEN e.direction = 'DEBIT' THEN -e.sessionCount
                ELSE 0
            END
        ), 0)
        FROM SessionCreditEntry e
        WHERE e.patient.id = :patientId
    """)
    Long sumCreditsByPatient(
        @Param("patientId")
        Long patientId
    );

    /**
     * Soma créditos de um paciente de um tipo específico
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN e.direction = 'CREDIT' THEN e.sessionCount
                WHEN e.direction = 'DEBIT' THEN -e.sessionCount
                ELSE 0
            END
        ), 0)
        FROM SessionCreditEntry e
        WHERE e.patient.id = :patientId
          AND e.entryType = :entryType
    """)
    Long sumCreditsByPatientAndType(
        @Param("patientId")
        Long patientId,

        @Param("entryType")
        SessionCreditEntryType entryType
    );
}
