package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.MedicalRecordRevision;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalRecordRevisionRepository
        extends JpaRepository<
                MedicalRecordRevision,
                UUID
        > {

    List<MedicalRecordRevision>
    findByMedicalRecordIdOrderByRevisionNumberDesc(
            UUID medicalRecordId
    );

    Optional<MedicalRecordRevision>
    findByMedicalRecordIdAndRevisionNumber(
            UUID medicalRecordId,
            Long revisionNumber
    );

    @Query("""
            SELECT COALESCE(
                MAX(r.revisionNumber),
                0
            )

            FROM MedicalRecordRevision r

            WHERE r.medicalRecord.id =
                :medicalRecordId
            """)
    Long findMaxRevisionNumber(
            @Param("medicalRecordId")
            UUID medicalRecordId
    );

    /**
     * 19. Verifica se psychoanalyst é o autor da revisão
     * Usado para autorizar leitura de revisão histórica
     */
    @Query("""
            SELECT CASE WHEN COUNT(r) > 0
                THEN TRUE
                ELSE FALSE
            END
            FROM MedicalRecordRevision r
            WHERE r.id = :revisionId
            AND r.authorPsychoanalyst.id = :psychoanalystId
            """)
    boolean existsByIdAndAuthorPsychoanalystId(
            @Param("revisionId") UUID revisionId,
            @Param("psychoanalystId") Long psychoanalystId
    );
}