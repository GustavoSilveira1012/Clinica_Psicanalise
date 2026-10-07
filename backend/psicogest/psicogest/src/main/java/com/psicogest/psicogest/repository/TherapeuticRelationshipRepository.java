package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.model.entity.TherapeuticRelationship;
import com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TherapeuticRelationshipRepository
                extends JpaRepository<TherapeuticRelationship, Long> {

        Optional<TherapeuticRelationship> findByIdAndPsychoanalystId(
                        Long id,
                        Long psychoanalystId);

        List<TherapeuticRelationship> findByPatientIdOrderByStartedAtDesc(
                        Long patientId);

        List<TherapeuticRelationship> findByPsychoanalystIdOrderByStartedAtDesc(
                        Long psychoanalystId);

        boolean existsByPatientIdAndPsychoanalystIdAndStatusIn(
                        Long patientId,
                        Long psychoanalystId,
                        Collection<TherapeuticRelationshipStatus> statuses);

        Optional<TherapeuticRelationship> findByPatientIdAndPsychoanalystIdAndStatus(
                        Long patientId,
                        Long psychoanalystId,
                        TherapeuticRelationshipStatus status);

        Optional<TherapeuticRelationship> findByPatientIdAndPrimaryTrueAndStatus(
                        Long patientId,
                        TherapeuticRelationshipStatus status);

        @Query("""
                SELECT DISTINCT patient
                FROM TherapeuticRelationship tr
                JOIN tr.patient patient
                WHERE tr.psychoanalyst.id = :psychoanalystId
                  AND tr.status IN :statuses
                  AND patient.active = true
                ORDER BY patient.id
                """)
        List<Patient> findAccessiblePatients(
                        @Param("psychoanalystId") Long psychoanalystId,
                        @Param("statuses") Collection<TherapeuticRelationshipStatus> statuses
        );

        @Query("""
                SELECT DISTINCT patient
                FROM TherapeuticRelationship tr
                JOIN tr.patient patient
                WHERE tr.psychoanalyst.id = :psychoanalystId
                  AND patient.organizationId = :organizationId
                  AND tr.status IN :statuses
                  AND (:active IS NULL OR patient.active = :active)
                  AND (:query IS NULL OR :query = ''
                       OR lower(patient.user.name) LIKE lower(concat('%', :query, '%'))
                       OR lower(patient.user.email) LIKE lower(concat('%', :query, '%')))
                ORDER BY patient.id
                """)
        List<Patient> searchAccessiblePatients(
                        @Param("psychoanalystId") Long psychoanalystId,
                        @Param("organizationId") UUID organizationId,
                        @Param("statuses") Collection<TherapeuticRelationshipStatus> statuses,
                        @Param("query") String query,
                        @Param("active") Boolean active
        );
}
