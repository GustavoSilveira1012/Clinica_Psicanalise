package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    boolean existsByUserId(Long userId);

    List<Patient> findByActiveTrue();
}