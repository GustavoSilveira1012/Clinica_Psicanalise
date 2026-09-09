package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.MedicalRecordAddendum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalRecordAddendumRepository
        extends JpaRepository<
                MedicalRecordAddendum,
                UUID
        > {

    List<MedicalRecordAddendum>
    findByMedicalRecordIdOrderByCreatedAtAsc(
            UUID medicalRecordId
    );

    Optional<MedicalRecordAddendum>
    findByIdAndMedicalRecordId(
            UUID id,
            UUID medicalRecordId
    );
}