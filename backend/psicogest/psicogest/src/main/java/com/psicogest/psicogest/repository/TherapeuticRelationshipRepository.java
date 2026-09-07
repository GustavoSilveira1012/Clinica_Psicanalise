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
                SELECT DISTINCT tr.patient
                FROM TherapeuticRelationship tr
                WHERE tr.psychoanalyst.id = :psychoanalystId
                  AND tr.status IN :statuses
                  AND tr.patient.active = true
                ORDER BY tr.patient.id
                """)
        List<Patient> findAccessiblePatients(
                        @Param("psychoanalystId") Long psychoanalystId,
                        @Param("statuses") Collection<TherapeuticRelationshipStatus> statuses
        );
}