package com.psicogest.psicogest.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psicogest.psicogest.model.entity.PackagePlanItem;

public interface PackagePlanItemRepository extends JpaRepository<PackagePlanItem, UUID> {

    List<PackagePlanItem> findByPackagePlanVersionId(UUID packagePlanVersionId);

    @Query("select coalesce(sum(i.quantity), 0) from PackagePlanItem i where i.packagePlanVersion.id = :versionId")
    Integer sumQuantityByVersionId(@Param("versionId") UUID versionId);
}
