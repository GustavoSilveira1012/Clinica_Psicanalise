package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Addendum;
import com.psicogest.psicogest.model.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddendumRepository extends JpaRepository<Addendum, UUID> {

    /**
     * Encontra addendum por ID
     */
    Optional<Addendum> findById(UUID id);

    /**
     * Lista todos os addendums de um prontuário (ordenados por data de criação)
     */
    List<Addendum> findByMedicalRecordOrderByCreatedAtDesc(MedicalRecord medicalRecord);

    /**
     * Conta quantos addendums um prontuário tem
     */
    int countByMedicalRecord(MedicalRecord medicalRecord);

    /**
     * Verifica se um addendum específico existe
     */
    boolean existsById(UUID id);
}
