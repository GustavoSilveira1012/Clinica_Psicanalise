package com.psicogest.psicogest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psicogest.psicogest.model.entity.ClinicUserMembership;
import com.psicogest.psicogest.model.enums.ClinicAccessRole;
import com.psicogest.psicogest.model.enums.ClinicUserMembershipStatus;

public interface ClinicUserMembershipRepository
        extends JpaRepository<
                ClinicUserMembership,
                Long
        > {

    boolean existsByClinicIdAndUserIdAndAccessRoleAndStatus(
            Long clinicId,
            Long userId,
            ClinicAccessRole accessRole,
            ClinicUserMembershipStatus status
    );

    List<ClinicUserMembership>
    findByUserIdAndStatus(
            Long userId,
            ClinicUserMembershipStatus status
    );
}