package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {
    boolean existsByCnpj(String cnpj);
}
