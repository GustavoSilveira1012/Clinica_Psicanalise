package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AvailabilityExceptionRepository
        extends JpaRepository<AvailabilityException, Long> {

    Optional<AvailabilityException>
    findByIdAndPsychoanalystId(
            Long id,
            Long psychoanalystId
    );

    List<AvailabilityException>
    findByPsychoanalystIdOrderByDateAscStartTimeAsc(
            Long psychoanalystId
    );

    List<AvailabilityException>
    findByPsychoanalystIdAndDateOrderByStartTimeAsc(
            Long psychoanalystId,
            LocalDate date
    );

    List<AvailabilityException>
    findByPsychoanalystIdAndDateBetweenOrderByDateAscStartTimeAsc(
            Long psychoanalystId,
            LocalDate startDate,
            LocalDate endDate
    );
}