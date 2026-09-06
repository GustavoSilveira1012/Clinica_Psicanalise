package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Psychoanalyst;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PsychoanalystRepository
        extends JpaRepository<Psychoanalyst, Long> {
    Boolean existsByUserId(Long userId);

    List<Psychoanalyst> findByActiveTrue();
}
