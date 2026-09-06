package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.lifecycle.LifecycleManager;
import com.psicogest.psicogest.dto.common.DeactivateDTO;
import com.psicogest.psicogest.dto.patient.PatientCreateDTO;
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
import com.psicogest.psicogest.model.enums.AppointmentStatus;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

        private final PatientRepository patientRepository;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final LifecycleManager lifecycleManager;
        private final AppointmentRepository appointmentRepository;

        public PatientService(
                        PatientRepository patientRepository,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        LifecycleManager lifecycleManager,
                        AppointmentRepository appointmentRepository) {
                this.patientRepository = patientRepository;
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.lifecycleManager = lifecycleManager;
                this.appointmentRepository = appointmentRepository;
        }

        public PatientResponseDTO create(PatientCreateDTO dto) {

                if (userRepository.existsByEmail(dto.email())) {

                        throw new EmailAlreadyExistsException(
                                        "Já existe um usuário cadastrado com este e-mail");
                }

                User user = User.builder()
                                .name(dto.name())
                                .email(dto.email())
                                .passwordHash(
                                                passwordEncoder.encode(dto.password()))
                                .role(UserRole.PATIENT)
                                .active(true)
                                .build();

                User savedUser = userRepository.save(user);

                Patient patient = Patient.builder()
                                .user(savedUser)
                                .phone(dto.phone())
                                .birthDate(dto.birthDate())
                                .build();

                Patient savedPatient = patientRepository.save(patient);

                return toResponseDTO(savedPatient);
        }

        public List<PatientResponseDTO> findAll() {

                return patientRepository.findByActiveTrue()
                                .stream()
                                .map(this::toResponseDTO)
                                .toList();
        }

        public PatientResponseDTO findById(Long id) {

                Patient patient = patientRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Paciente não encontrado com o ID: " + id));

                return toResponseDTO(patient);
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
                return patientRepository.findById(patientId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Paciente não encontrado com o ID: " + patientId));
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
                                user.getActive());
        }
}