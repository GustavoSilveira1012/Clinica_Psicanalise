package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.ClinicMembershipPeriod;
import com.psicogest.psicogest.model.enums.ClinicMembershipPeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClinicMembershipPeriodRepository
                extends JpaRepository<ClinicMembershipPeriod, Long> {

        List<ClinicMembershipPeriod> findByMembershipIdOrderByStartedAtDesc(
                        Long membershipId);

        Optional<ClinicMembershipPeriod> findByMembershipIdAndStatus(
                        Long membershipId,
                        ClinicMembershipPeriodStatus status);

        Optional<ClinicMembershipPeriod> findByIdAndMembershipId(
                        Long periodId,
                        Long membershipId);

        boolean existsByMembershipIdAndStatus(
                        Long membershipId,
                        ClinicMembershipPeriodStatus status);

        boolean existsByMembershipIdAndStartedAtLessThanEqualAndEndedAtIsNull(
                        Long membershipId,
                        LocalDateTime instant);

        @Query("""
                        SELECT COUNT(p) > 0
                        FROM ClinicMembershipPeriod p
                        WHERE p.membership.id = :membershipId
                          AND p.startedAt <= :instant
                          AND (
                                p.endedAt IS NULL
                                OR p.endedAt > :instant
                          )
                        """)
        boolean isActiveAt(
                        @Param("membershipId") Long membershipId,
                        @Param("instant") LocalDateTime instant);
}