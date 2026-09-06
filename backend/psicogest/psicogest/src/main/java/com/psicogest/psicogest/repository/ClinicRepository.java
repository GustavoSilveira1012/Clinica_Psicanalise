package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {
    boolean existsByCnpj(String cnpj);

    List<Clinic> findByActiveTrue();
}
