package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Psychoanalyst;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PsychoanalystRepository
        extends JpaRepository<Psychoanalyst, Long> {
    Boolean existsByUserId(Long userId);
}
