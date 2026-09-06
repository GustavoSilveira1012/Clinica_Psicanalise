package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.AppointmentSeries;
import com.psicogest.psicogest.model.enums.AppointmentSeriesStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentSeriesRepository
        extends JpaRepository<
                AppointmentSeries,
                UUID
        > {

    Optional<AppointmentSeries>
    findByIdAndPsychoanalystId(
            UUID id,
            Long psychoanalystId
    );

    List<AppointmentSeries>
    findByPsychoanalystIdAndStatusOrderByStartsOnAsc(
            Long psychoanalystId,
            AppointmentSeriesStatus status
    );

    List<AppointmentSeries>
    findByPatientIdOrderByStartsOnDesc(
            Long patientId
    );
}