package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.lifecycle.LifecycleManager;
import com.psicogest.psicogest.dto.common.DeactivateDTO;
import com.psicogest.psicogest.dto.patient.PatientCreateDTO;
import com.psicogest.psicogest.dto.patient.PatientUpdateDTO;
import com.psicogest.psicogest.dto.patient.PatientResponseDTO;
import com.psicogest.psicogest.exception.EmailAlreadyExistsException;
import com.psicogest.psicogest.exception.EntityLifecycleException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.UserRole;
import com.psicogest.psicogest.repository.PatientRepository;
import com.psicogest.psicogest.repository.AppointmentRepository;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import com.psicogest.psicogest.dto.relationship.TherapeuticRelationshipCreateDTO;
import org.springframework.security.access.AccessDeniedException;
import com.psicogest.psicogest.model.enums.AppointmentStatus;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import com.psicogest.psicogest.exception.TenantContextRequiredException;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;

@Service
public class PatientService {

        private final PatientRepository patientRepository;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final LifecycleManager lifecycleManager;
        private final AppointmentRepository appointmentRepository;
        private final PsychoanalystRepository psychoanalystRepository;
        private final TherapeuticRelationshipService relationshipService;

        public PatientService(
                        PatientRepository patientRepository,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        LifecycleManager lifecycleManager,
                        AppointmentRepository appointmentRepository,
                        PsychoanalystRepository psychoanalystRepository,
                        TherapeuticRelationshipService relationshipService) {
                this.patientRepository = patientRepository;
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.lifecycleManager = lifecycleManager;
                this.appointmentRepository = appointmentRepository;
                this.psychoanalystRepository = psychoanalystRepository;
                this.relationshipService = relationshipService;
        }

        @Transactional
        public PatientResponseDTO create(PatientCreateDTO dto) {

                Long responsibleProfessionalId = null;
                if (Boolean.TRUE.equals(dto.linkToCurrentProfessional())) {
                        TenantContext tenant = TenantContextHolder.get();
                        var actor = tenant == null ? null : userRepository.findById(tenant.userId()).orElse(null);
                        if (actor == null || actor.getRole() != UserRole.PSYCHOANALYST) {
                                throw new AccessDeniedException("Somente o profissional pode iniciar seu próprio vínculo clínico");
                        }
                        var professional = psychoanalystRepository.findByUserId(actor.getId())
                                        .filter(item -> Boolean.TRUE.equals(item.getActive())
                                                        && organizationId().equals(item.getOrganizationId()))
                                        .orElseThrow(() -> new AccessDeniedException("Profissional não disponível nesta organização"));
                        responsibleProfessionalId = professional.getId();
                }

                String email = dto.email().trim().toLowerCase(java.util.Locale.ROOT);
                if (userRepository.existsByEmailIgnoreCase(email)) {

                        throw new EmailAlreadyExistsException(
                                        "Já existe um usuário cadastrado com este e-mail");
                }

                User user = User.builder()
                                .name(dto.name())
                                .email(email)
                                .passwordHash(
                                                passwordEncoder.encode(dto.password()))
                                .role(UserRole.PATIENT)
                                .active(true)
                                .build();

                User savedUser = userRepository.save(user);

                Patient patient = Patient.builder()
                                .organizationId(organizationId())
                                .user(savedUser)
                                .phone(dto.phone())
                                .birthDate(dto.birthDate())
                                .build();

                Patient savedPatient = patientRepository.save(patient);

                if (responsibleProfessionalId != null) {
                        relationshipService.create(responsibleProfessionalId,
                                        new TherapeuticRelationshipCreateDTO(savedPatient.getId(), true, null));
                }

                return toResponseDTO(savedPatient);
        }

        public List<PatientResponseDTO> findAll() {

                return patientRepository.findByOrganizationIdAndActiveTrue(organizationId())
                                .stream()
                                .map(this::toResponseDTO)
                                .toList();
        }

        public PatientResponseDTO findById(Long id) {

                Patient patient = patientRepository.findByIdAndOrganizationId(id, organizationId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Paciente não encontrado com o ID: " + id));

                return toResponseDTO(patient);
        }

        @Transactional
        public PatientResponseDTO update(Long patientId, PatientUpdateDTO dto) {
                Patient patient = findPatientById(patientId);
                User user = patient.getUser();

                if (dto.email() != null) {
                        String email = dto.email().trim().toLowerCase(java.util.Locale.ROOT);
                        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
                                throw new EmailAlreadyExistsException("Já existe um usuário cadastrado com este e-mail");
                        }
                        user.setEmail(email);
                }
                if (dto.name() != null) user.setName(dto.name().trim());
                if (dto.phone() != null) patient.setPhone(dto.phone().trim());
                if (dto.birthDate() != null) patient.setBirthDate(dto.birthDate());

                userRepository.save(user);
                return toResponseDTO(patientRepository.save(patient));
        }

        @Transactional
        public PatientResponseDTO deactivate(Long patientId, DeactivateDTO dto) {
                Patient patient = findPatientById(patientId);
                boolean hasFutureAppointments = appointmentRepository
                                .existsByPatientIdAndStatusInAndScheduledStartAfter(
                                                patientId,
                                                java.util.EnumSet.of(
                                                                AppointmentStatus.SCHEDULED,
                                                                AppointmentStatus.CONFIRMED),
                                                LocalDateTime.now());
                if (hasFutureAppointments) {
                        throw new EntityLifecycleException(
                                        "O paciente possui consultas futuras. Cancele ou reagende essas consultas antes da desativação.");
                }
                lifecycleManager.deactivate(patient, dto.reason());
                return toResponseDTO(patientRepository.save(patient));
        }

        @Transactional
        public PatientResponseDTO reactivate(Long patientId) {
                Patient patient = findPatientById(patientId);
                lifecycleManager.reactivate(patient);
                return toResponseDTO(patientRepository.save(patient));
        }

        private Patient findPatientById(Long patientId) {
                return patientRepository.findByIdAndOrganizationId(patientId, organizationId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Paciente não encontrado com o ID: " + patientId));
        }

        private java.util.UUID organizationId() {
                TenantContext tenant = TenantContextHolder.get();
                if (tenant == null || tenant.organizationId() == null) {
                        throw new TenantContextRequiredException("Contexto de organização obrigatório");
                }
                return tenant.organizationId();
        }

        private PatientResponseDTO toResponseDTO(
                        Patient patient) {

                User user = patient.getUser();

                return new PatientResponseDTO(
                                patient.getId(),
                                user.getName(),
                                user.getEmail(),
                                patient.getPhone(),
                                patient.getBirthDate(),
                                patient.getActive());
        }
}
