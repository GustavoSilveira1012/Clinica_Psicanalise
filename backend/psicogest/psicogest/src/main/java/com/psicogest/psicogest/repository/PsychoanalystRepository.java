package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Psychoanalyst;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PsychoanalystRepository
        extends JpaRepository<Psychoanalyst, Long> {
    boolean existsByIdAndUserId(
        Long psychoanalystId,
        Long userId
);

Optional<Psychoanalyst>
findByUserId(
        Long userId
);

    List<Psychoanalyst> findByActiveTrue();

    /**
     * Verifica se psicanalista tem vínculo ACTIVE com paciente
     */
    @Query("""
            SELECT COUNT(tr) > 0
            FROM TherapeuticRelationship tr
            WHERE tr.psychoanalyst.id = :psychoanalystId
              AND tr.patient.id = :patientId
              AND tr.status = com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus.ACTIVE
            """)
    boolean existsActiveTherapeuticRelationship(
            @Param("psychoanalystId") Long psychoanalystId,
            @Param("patientId") Long patientId
    );

    /**
     * Verifica se psicanalista tem vínculo ACTIVE ou SUSPENDED com paciente
     */
    @Query("""
            SELECT COUNT(tr) > 0
            FROM TherapeuticRelationship tr
            WHERE tr.psychoanalyst.id = :psychoanalystId
              AND tr.patient.id = :patientId
              AND tr.status IN (
                com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus.ACTIVE,
                com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus.SUSPENDED
              )
            """)
    boolean existsTherapeuticRelationship(
            @Param("psychoanalystId") Long psychoanalystId,
            @Param("patientId") Long patientId
    );
}
