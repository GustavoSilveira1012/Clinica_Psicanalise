package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.clinic.ClinicMembershipPeriodStateMachine;
import com.psicogest.psicogest.dto.clinic.*;
import com.psicogest.psicogest.exception.*;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.*;
import com.psicogest.psicogest.repository.*;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClinicMembershipPeriodService {

    private final ClinicMembershipRepository membershipRepository;

    private final ClinicMembershipPeriodRepository periodRepository;

    private final ClinicMembershipPeriodStateMachine stateMachine;

    public ClinicMembershipPeriodService(
            ClinicMembershipRepository membershipRepository,
            ClinicMembershipPeriodRepository periodRepository,
            ClinicMembershipPeriodStateMachine stateMachine
    ) {

        this.membershipRepository =
                membershipRepository;

        this.periodRepository =
                periodRepository;

        this.stateMachine =
                stateMachine;
    }

    @Transactional
    public ClinicMembershipPeriodResponseDTO start(
            Long membershipId,
            ClinicMembershipPeriodCreateDTO dto
    ) {
        ClinicMembership membership = findMembership(membershipId);

        if (periodRepository.existsByMembershipIdAndStatus(
                membershipId,
                ClinicMembershipPeriodStatus.ACTIVE
        )) {
            throw new ClinicMembershipPeriodConflictException(
                    "Este vínculo já possui um período ativo"
            );
        }

        LocalDateTime start = dto.startedAt() != null
                ? dto.startedAt()
                : LocalDateTime.now();

        if (start.isAfter(LocalDateTime.now())) {
            throw new ClinicMembershipPeriodConflictException(
                    "O período não pode iniciar no futuro"
            );
        }

        ClinicMembershipPeriod period = ClinicMembershipPeriod.builder()
                .membership(membership)
                .status(ClinicMembershipPeriodStatus.ACTIVE)
                .startedAt(start)
                .build();

        ClinicMembershipPeriod saved = saveSafely(period);
        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);

        return toResponseDTO(saved);
    }

    @Transactional
    public ClinicMembershipPeriodResponseDTO end(
            Long membershipId,
            Long periodId,
            ClinicMembershipPeriodEndDTO dto
    ) {
        ClinicMembershipPeriod period = findPeriod(membershipId, periodId);

        stateMachine.validateTransition(
                period.getStatus(),
                ClinicMembershipPeriodStatus.ENDED
        );

        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(period.getStartedAt())) {
            throw new ClinicMembershipPeriodConflictException(
                    "O encerramento deve ocorrer após o início do vínculo"
            );
        }

        period.setStatus(ClinicMembershipPeriodStatus.ENDED);
        period.setEndedAt(now);
        period.setEndReason(dto.reason().trim());

        ClinicMembershipPeriod saved = periodRepository.saveAndFlush(period);
        ClinicMembership membership = period.getMembership();
        membership.setStatus(MembershipStatus.INACTIVE);
        membershipRepository.save(membership);

        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ClinicMembershipPeriodResponseDTO> findAll(
            Long membershipId
    ) {
        findMembership(membershipId);
        return periodRepository.findByMembershipIdOrderByStartedAtDesc(membershipId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isActiveNow(Long membershipId) {
        return periodRepository.isActiveAt(membershipId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public boolean isActiveAt(
            Long membershipId,
            LocalDateTime instant
    ) {
        return periodRepository.isActiveAt(membershipId, instant);
    }

    private ClinicMembership findMembership(Long membershipId) {
        return membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vínculo com clínica não encontrado"
                ));
    }

    private ClinicMembershipPeriod findPeriod(
            Long membershipId,
            Long periodId
    ) {
        return periodRepository.findByIdAndMembershipId(periodId, membershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Período de vínculo não encontrado"
                ));
    }

    private ClinicMembershipPeriod saveSafely(
            ClinicMembershipPeriod period
    ) {
        try {
            return periodRepository.saveAndFlush(period);
        } catch (DataIntegrityViolationException exception) {
            throw new ClinicMembershipPeriodConflictException(
                    "O período informado conflita com outro período deste vínculo"
            );
        }
    }

    private ClinicMembershipPeriodResponseDTO toResponseDTO(
            ClinicMembershipPeriod period
    ) {
        ClinicMembership membership = period.getMembership();

        return new ClinicMembershipPeriodResponseDTO(
                period.getId(),
                membership.getId(),
                membership.getClinic().getId(),
                membership.getClinic().getName(),
                membership.getPsychoanalyst().getId(),
                membership.getPsychoanalyst().getUser().getName(),
                period.getStatus(),
                period.getStartedAt(),
                period.getEndedAt(),
                period.getEndReason()
        );
    }
}