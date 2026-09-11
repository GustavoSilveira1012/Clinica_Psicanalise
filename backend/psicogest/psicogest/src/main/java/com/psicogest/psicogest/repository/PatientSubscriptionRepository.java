package com.psicogest.psicogest.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psicogest.psicogest.model.entity.PatientSubscription;
import com.psicogest.psicogest.model.enums.SubscriptionStatus;

import jakarta.persistence.LockModeType;

public interface PatientSubscriptionRepository extends JpaRepository<PatientSubscription, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PatientSubscription s where s.id = :id")
    Optional<PatientSubscription> findByIdForUpdate(@Param("id") UUID id);

    @Query("select s.id from PatientSubscription s where s.status in ('PENDING_START', 'ACTIVE', 'PAST_DUE') and s.nextCycleStart <= :today")
    List<UUID> findIdsReadyForCycle(@Param("today") LocalDate today);

    List<PatientSubscription> findByPatientId(Long patientId);
}
