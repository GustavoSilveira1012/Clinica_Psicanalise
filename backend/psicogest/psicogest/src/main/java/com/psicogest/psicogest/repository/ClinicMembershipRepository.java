package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.ClinicMembership;
import com.psicogest.psicogest.model.enums.MembershipStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicMembershipRepository
                extends JpaRepository<ClinicMembership, Long> {

        boolean existsByClinicIdAndPsychoanalystId(
                        Long clinicId,
                        Long psychoanalystId);

        List<ClinicMembership> findByClinicId(Long clinicId);

        List<ClinicMembership> findByPsychoanalystId(Long psychoanalystId);

        Optional<ClinicMembership> findByIdAndPsychoanalystId(
                        Long id,
                        Long psychoanalystId);

        boolean existsByIdAndPsychoanalystIdAndStatus(
                        Long id,
                        Long psychoanalystId,
                        MembershipStatus status);
}
