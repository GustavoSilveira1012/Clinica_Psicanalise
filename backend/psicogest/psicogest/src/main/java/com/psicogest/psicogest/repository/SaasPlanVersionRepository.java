package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.saas.SaasPlanVersion;
import com.psicogest.psicogest.model.enums.SaasPlanVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SaasPlanVersionRepository extends JpaRepository<SaasPlanVersion, UUID> {

    @Query("""
            select v from SaasPlanVersion v
            join fetch v.plan p
            where p.code = :planCode
              and p.active = true
              and v.status = :status
            order by v.version desc
            """)
    Optional<SaasPlanVersion> findPublishedByPlanCode(
            @Param("planCode") String planCode,
            @Param("status") SaasPlanVersionStatus status);
}
