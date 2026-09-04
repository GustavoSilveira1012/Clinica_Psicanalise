package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.ClinicMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClinicMembershipRepository
        extends JpaRepository<ClinicMembership, Long> {

    boolean existsByClinicIdAndPsychoanalystId(
            Long clinicId,
            Long psychoanalystId
    );

    List<ClinicMembership> findByClinicId(Long clinicId);

    List<ClinicMembership> findByPsychoanalystId(Long psychoanalystId);
}
