package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.relationship.TherapeuticRelationshipStateMachine;
import com.psicogest.psicogest.dto.relationship.*;
import com.psicogest.psicogest.exception.*;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus;
import com.psicogest.psicogest.repository.*;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
public class TherapeuticRelationshipService {

        private static final EnumSet<TherapeuticRelationshipStatus> CURRENT_STATUSES = EnumSet.of(
                        TherapeuticRelationshipStatus.ACTIVE,
                        TherapeuticRelationshipStatus.SUSPENDED);

        private final TherapeuticRelationshipRepository relationshipRepository;

        private final PatientRepository patientRepository;

        private final PsychoanalystRepository psychoanalystRepository;

        private final TherapeuticRelationshipStateMachine stateMachine;

        public TherapeuticRelationshipService(
                        TherapeuticRelationshipRepository relationshipRepository,
                        PatientRepository patientRepository,
                        PsychoanalystRepository psychoanalystRepository,
                        TherapeuticRelationshipStateMachine stateMachine) {

                this.relationshipRepository = relationshipRepository;

                this.patientRepository = patientRepository;

                this.psychoanalystRepository = psychoanalystRepository;

                this.stateMachine = stateMachine;
        }

        @Transactional
        public TherapeuticRelationshipResponseDTO create(
                        Long psychoanalystId,
                        TherapeuticRelationshipCreateDTO dto) {
                Psychoanalyst psychoanalyst = findPsychoanalyst(psychoanalystId);
                Patient patient = findPatient(dto.patientId());

                if (relationshipRepository
                                .existsByPatientIdAndPsychoanalystIdAndStatusIn(
                                                dto.patientId(),
                                                psychoanalystId,
                                                CURRENT_STATUSES)) {
                        throw new TherapeuticRelationshipConflictException(
                                        "Já existe um vínculo terapêutico atual entre este paciente e este psicanalista");
                }

                boolean primary = Boolean.TRUE.equals(dto.primary());
                if (primary) {
                        validatePrimaryAvailability(dto.patientId());
                }

                LocalDateTime startedAt = dto.startedAt() != null
                                ? dto.startedAt()
                                : LocalDateTime.now();

                if (startedAt.isAfter(LocalDateTime.now())) {
                        throw new TherapeuticRelationshipConflictException(
                                        "O vínculo não pode começar no futuro");
                }

                TherapeuticRelationship relationship = TherapeuticRelationship.builder()
                                .patient(patient)
                                .psychoanalyst(psychoanalyst)
                                .status(TherapeuticRelationshipStatus.ACTIVE)
                                .primary(primary)
                                .startedAt(startedAt)
                                .build();

                return toResponseDTO(saveSafely(relationship));
        }

        private void validatePrimaryAvailability(Long patientId) {
                if (relationshipRepository
                                .findByPatientIdAndPrimaryTrueAndStatus(
                                                patientId,
                                                TherapeuticRelationshipStatus.ACTIVE)
                                .isPresent()) {
                        throw new TherapeuticRelationshipConflictException(
                                        "O paciente já possui um psicanalista principal ativo");
                }
        }

        private TherapeuticRelationship saveSafely(
                        TherapeuticRelationship relationship) {
                try {
                        return relationshipRepository.saveAndFlush(relationship);
                } catch (DataIntegrityViolationException exception) {
                        throw new TherapeuticRelationshipConflictException(
                                        "Não foi possível criar o vínculo devido a um relacionamento clínico conflitante");
                }
        }

        @Transactional
        public TherapeuticRelationshipResponseDTO suspend(
                        Long psychoanalystId,
                        Long relationshipId) {
                TherapeuticRelationship relationship = findRelationship(
                                psychoanalystId,
                                relationshipId);
                stateMachine.validateTransition(
                                relationship.getStatus(),
                                TherapeuticRelationshipStatus.SUSPENDED);
                relationship.setStatus(TherapeuticRelationshipStatus.SUSPENDED);
                return toResponseDTO(relationshipRepository.save(relationship));
        }

        @Transactional
        public TherapeuticRelationshipResponseDTO resume(
                        Long psychoanalystId,
                        Long relationshipId) {
                TherapeuticRelationship relationship = findRelationship(
                                psychoanalystId,
                                relationshipId);
                stateMachine.validateTransition(
                                relationship.getStatus(),
                                TherapeuticRelationshipStatus.ACTIVE);

                if (Boolean.TRUE.equals(relationship.getPrimary())) {
                        relationshipRepository
                                        .findByPatientIdAndPrimaryTrueAndStatus(
                                                        relationship.getPatient().getId(),
                                                        TherapeuticRelationshipStatus.ACTIVE)
                                        .filter(current -> !current.getId().equals(relationship.getId()))
                                        .ifPresent(ignored -> {
                                                throw new TherapeuticRelationshipConflictException(
                                                                "O paciente já possui outro profissional principal ativo");
                                        });
                }

                relationship.setStatus(TherapeuticRelationshipStatus.ACTIVE);
                return toResponseDTO(saveSafely(relationship));
        }

        @Transactional
        public TherapeuticRelationshipResponseDTO end(
                        Long psychoanalystId,
                        Long relationshipId,
                        TherapeuticRelationshipEndDTO dto) {
                TherapeuticRelationship relationship = findRelationship(
                                psychoanalystId,
                                relationshipId);
                stateMachine.validateTransition(
                                relationship.getStatus(),
                                TherapeuticRelationshipStatus.ENDED);
                relationship.setStatus(TherapeuticRelationshipStatus.ENDED);
                relationship.setEndedAt(LocalDateTime.now());
                relationship.setEndReason(dto.reason().trim());
                relationship.setPrimary(false);
                return toResponseDTO(relationshipRepository.save(relationship));
        }

        @Transactional
        public TherapeuticRelationshipResponseDTO makePrimary(
                        Long psychoanalystId,
                        Long relationshipId) {
                TherapeuticRelationship relationship = findRelationship(
                                psychoanalystId,
                                relationshipId);

                if (relationship.getStatus() != TherapeuticRelationshipStatus.ACTIVE) {
                        throw new TherapeuticRelationshipConflictException(
                                        "Somente um vínculo ativo pode ser definido como principal");
                }

                if (Boolean.TRUE.equals(relationship.getPrimary())) {
                        return toResponseDTO(relationship);
                }

                relationshipRepository
                                .findByPatientIdAndPrimaryTrueAndStatus(
                                                relationship.getPatient().getId(),
                                                TherapeuticRelationshipStatus.ACTIVE)
                                .ifPresent(previous -> {
                                        previous.setPrimary(false);
                                        relationshipRepository.save(previous);
                                });

                relationship.setPrimary(true);
                return toResponseDTO(saveSafely(relationship));
        }

        @Transactional(readOnly = true)
        public List<TherapeuticRelationshipResponseDTO> findByPatient(
                        Long patientId) {
                findPatient(patientId);
                return relationshipRepository.findByPatientIdOrderByStartedAtDesc(patientId)
                                .stream()
                                .map(this::toResponseDTO)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<TherapeuticRelationshipResponseDTO> findByPsychoanalyst(
                        Long psychoanalystId) {
                findPsychoanalyst(psychoanalystId);
                return relationshipRepository
                                .findByPsychoanalystIdOrderByStartedAtDesc(psychoanalystId)
                                .stream()
                                .map(this::toResponseDTO)
                                .toList();
        }

        @Transactional(readOnly = true)
        public boolean hasCurrentRelationship(
                        Long patientId,
                        Long psychoanalystId) {
                return relationshipRepository
                                .existsByPatientIdAndPsychoanalystIdAndStatusIn(
                                                patientId,
                                                psychoanalystId,
                                                CURRENT_STATUSES);
        }

        @Transactional(readOnly = true)
        public boolean hasActiveRelationship(
                        Long patientId,
                        Long psychoanalystId) {
                return relationshipRepository
                                .findByPatientIdAndPsychoanalystIdAndStatus(
                                                patientId,
                                                psychoanalystId,
                                                TherapeuticRelationshipStatus.ACTIVE)
                                .isPresent();
        }

        private Patient findPatient(Long id) {
                return patientRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Paciente não encontrado"));
        }

        private Psychoanalyst findPsychoanalyst(Long id) {
                return psychoanalystRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Psicanalista não encontrado"));
        }

        private TherapeuticRelationship findRelationship(
                        Long psychoanalystId,
                        Long relationshipId) {
                return relationshipRepository
                                .findByIdAndPsychoanalystId(relationshipId, psychoanalystId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Vínculo terapêutico não encontrado"));
        }

        private TherapeuticRelationshipResponseDTO toResponseDTO(
                        TherapeuticRelationship relationship) {
                return new TherapeuticRelationshipResponseDTO(
                                relationship.getId(),
                                relationship.getPatient().getId(),
                                relationship.getPatient().getUser().getName(),
                                relationship.getPsychoanalyst().getId(),
                                relationship.getPsychoanalyst().getUser().getName(),
                                relationship.getStatus(),
                                relationship.getPrimary(),
                                relationship.getStartedAt(),
                                relationship.getEndedAt(),
                                relationship.getEndReason());
        }
}