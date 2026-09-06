package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.clinic.ClinicMembershipCreateDTO;
import com.psicogest.psicogest.dto.clinic.ClinicMembershipResponseDTO;
import com.psicogest.psicogest.exception.MembershipAlreadyExistsException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.Clinic;
import com.psicogest.psicogest.model.entity.ClinicMembership;
import com.psicogest.psicogest.model.entity.ClinicMembershipPeriod;
import com.psicogest.psicogest.model.entity.Psychoanalyst;
import com.psicogest.psicogest.model.enums.ClinicMembershipPeriodStatus;
import com.psicogest.psicogest.model.enums.MembershipStatus;
import com.psicogest.psicogest.repository.ClinicMembershipRepository;
import com.psicogest.psicogest.repository.ClinicMembershipPeriodRepository;
import com.psicogest.psicogest.repository.ClinicRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClinicMembershipService {

        private final ClinicMembershipRepository membershipRepository;
        private final ClinicMembershipPeriodRepository periodRepository;
        private final ClinicRepository clinicRepository;
        private final PsychoanalystRepository psychoanalystRepository;

        public ClinicMembershipService(
                        ClinicMembershipRepository membershipRepository,
                        ClinicMembershipPeriodRepository periodRepository,
                        ClinicRepository clinicRepository,
                        PsychoanalystRepository psychoanalystRepository) {
                this.membershipRepository = membershipRepository;
                this.periodRepository = periodRepository;
                this.clinicRepository = clinicRepository;
                this.psychoanalystRepository = psychoanalystRepository;
        }

        @Transactional
        public ClinicMembershipResponseDTO create(
                        Long clinicId,
                        ClinicMembershipCreateDTO dto) {

                Clinic clinic = clinicRepository.findById(clinicId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Clínica não encontrada com o ID: " + clinicId));

                Psychoanalyst psychoanalyst = psychoanalystRepository.findById(dto.psychoanalystId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Psicanalista não encontrado com o ID: "
                                                                + dto.psychoanalystId()));

                if (membershipRepository
                                .existsByClinicIdAndPsychoanalystId(
                                                clinicId,
                                                dto.psychoanalystId())) {

                        throw new MembershipAlreadyExistsException(
                                        "Este psicanalista já possui vínculo com esta clínica");
                }

                ClinicMembership membership = ClinicMembership.builder()
                                .clinic(clinic)
                                .psychoanalyst(psychoanalyst)
                                .status(MembershipStatus.ACTIVE)
                                .build();

                ClinicMembership savedMembership = membershipRepository.save(membership);

                ClinicMembershipPeriod firstPeriod = ClinicMembershipPeriod.builder()
                                .membership(savedMembership)
                                .status(ClinicMembershipPeriodStatus.ACTIVE)
                                .startedAt(java.time.LocalDateTime.now())
                                .build();

                periodRepository.save(firstPeriod);

                return toResponseDTO(savedMembership);
        }

        public List<ClinicMembershipResponseDTO> findByClinicId(
                        Long clinicId) {

                if (!clinicRepository.existsById(clinicId)) {
                        throw new ResourceNotFoundException(
                                        "Clínica não encontrada com o ID: " + clinicId);
                }

                return membershipRepository.findByClinicId(clinicId)
                                .stream()
                                .map(this::toResponseDTO)
                                .toList();
        }

        private ClinicMembershipResponseDTO toResponseDTO(
                        ClinicMembership membership) {

                return new ClinicMembershipResponseDTO(
                                membership.getId(),
                                membership.getClinic().getId(),
                                membership.getClinic().getName(),
                                membership.getPsychoanalyst().getId(),
                                membership.getPsychoanalyst().getUser().getName(),
                                membership.getStatus(),
                                membership.getJoinedAt());
        }
}