package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.MedicalRecord;
import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.model.entity.Psychoanalyst;
import com.psicogest.psicogest.model.entity.TherapeuticRelationship;
import com.psicogest.psicogest.model.enums.MedicalRecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalRecordRepository
        extends JpaRepository<
                MedicalRecord,
                UUID
        > {

    Optional<MedicalRecord>
    findByIdAndPatientId(
            UUID id,
            Long patientId
    );

    List<MedicalRecord>
    findByPatientOrderByCreatedAtDesc(
            Patient patient
    );

    List<MedicalRecord>
    findByPatientAndStatusOrderByCreatedAtDesc(
            Patient patient,
            MedicalRecordStatus status
    );

    List<MedicalRecord>
    findByAuthorPsychoanalystOrderByCreatedAtDesc(
            Psychoanalyst psychoanalyst
    );

    List<MedicalRecord>
    findByTherapeuticRelationshipOrderByCreatedAtDesc(
            TherapeuticRelationship relationship
    );

    List<MedicalRecord>
    findByAppointmentIdOrderByCreatedAtDesc(
            Long appointmentId
    );

    List<MedicalRecord>
    findByPatientAndCreatedAtBetweenOrderByCreatedAtDesc(
            Patient patient,
            Instant startDate,
            Instant endDate
    );

    int countByPatientAndStatus(
            Patient patient,
            MedicalRecordStatus status
    );

    boolean existsByAppointmentId(
            Long appointmentId
    );

    /**
     * 27. Query de autorização para leitura
     *
     * Verifica se o psicanalista pode ler o prontuário:
     * - Se é o autor original OU
     * - Se tem vínculo ACTIVE ou SUSPENDED com o paciente
     */
    @Query(
            """
            SELECT COUNT(m) > 0
            FROM MedicalRecord m
            WHERE m.id = :recordId
              AND (
                m.authorPsychoanalyst.id = :psychoanalystId
                OR EXISTS (
                  SELECT tr.id
                  FROM TherapeuticRelationship tr
                  WHERE tr.patient.id = m.patient.id
                    AND tr.psychoanalyst.id = :psychoanalystId
                    AND tr.status IN (
                      com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus.ACTIVE,
                      com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus.SUSPENDED
                    )
                )
              )
            """
    )
    boolean existsReadableBy(
            @Param( "recordId" )
            UUID recordId,

            @Param( "psychoanalystId" )
            Long psychoanalystId
    );

    /**
     * 43. Query de autorização para editar
     *
     * Verifica se psicanalista pode editar:
     * - É o autor original OU
     * - Prontuário está em status DRAFT
     */
    boolean existsByIdAndAuthorPsychoanalystIdAndStatus(
            UUID id,
            Long psychoanalystId,
            MedicalRecordStatus status
    );
}