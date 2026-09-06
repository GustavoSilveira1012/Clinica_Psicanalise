package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.DayOfWeek;
import java.util.Optional;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
     List<Availability> findByPsychoanalystId(
            Long psychoanalystId
    );

    List<Availability> findByPsychoanalystIdAndActiveTrue(
            Long psychoanalystId
    );

    List<Availability> findByPsychoanalystIdAndDayOfWeekAndActiveTrue(
            Long psychoanalystId,
            DayOfWeek dayOfWeek
    );

    Optional<Availability> findByIdAndPsychoanalystId(
        Long id,
        Long psychoanalystId
        );
}
