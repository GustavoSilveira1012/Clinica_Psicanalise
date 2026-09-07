package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Psychoanalyst;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PsychoanalystRepository
        extends JpaRepository<Psychoanalyst, Long> {
    boolean existsByIdAndUserId(
        Long psychoanalystId,
        Long userId
);

Optional<Psychoanalyst>
findByUserId(
        Long userId
);

    List<Psychoanalyst> findByActiveTrue();
}
