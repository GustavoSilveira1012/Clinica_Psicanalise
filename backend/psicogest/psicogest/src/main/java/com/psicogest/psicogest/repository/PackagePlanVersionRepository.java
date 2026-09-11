package com.psicogest.psicogest.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psicogest.psicogest.model.entity.PackagePlanVersion;

import jakarta.persistence.LockModeType;

public interface PackagePlanVersionRepository extends JpaRepository<PackagePlanVersion, UUID> {

    @Query("select v from PackagePlanVersion v where v.id = :id and v.status = 'PUBLISHED'")
    Optional<PackagePlanVersion> findPublishedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from PackagePlanVersion v where v.id = :id")
    Optional<PackagePlanVersion> findByIdForUpdate(@Param("id") UUID id);
}
