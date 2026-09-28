package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    boolean existsByIdAndUserId(
        Long patientId,
        Long userId
);

Optional<Patient>
findByUserId(
        Long userId
);

Optional<Patient> findByUserIdAndOrganizationId(Long userId, UUID organizationId);

Optional<Patient> findByIdAndOrganizationId(Long id, UUID organizationId);

    List<Patient> findByOrganizationIdAndActiveTrue(UUID organizationId);

    @Query("""
            SELECT p FROM Patient p
            WHERE p.organizationId = :organizationId
              AND (:active IS NULL OR p.active = :active)
              AND (:query IS NULL OR :query = ''
                   OR lower(p.user.name) LIKE lower(concat('%', :query, '%'))
                   OR lower(p.user.email) LIKE lower(concat('%', :query, '%')))
            """)
    Page<Patient> searchByOrganization(
            @Param("organizationId") UUID organizationId,
            @Param("query") String query,
            @Param("active") Boolean active,
            Pageable pageable);
}
