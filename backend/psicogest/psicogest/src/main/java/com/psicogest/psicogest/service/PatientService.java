package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.patient.PatientCreateDTO;
import com.psicogest.psicogest.dto.patient.PatientResponseDTO;
import com.psicogest.psicogest.exception.EmailAlreadyExistsException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.UserRole;
import com.psicogest.psicogest.repository.PatientRepository;
import com.psicogest.psicogest.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PatientService(
            PatientRepository patientRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PatientResponseDTO create(PatientCreateDTO dto) {

        if (userRepository.existsByEmail(dto.email())) {

            throw new EmailAlreadyExistsException(
                    "Já existe um usuário cadastrado com este e-mail"
            );
        }

        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .passwordHash(
                        passwordEncoder.encode(dto.password())
                )
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

        return patientRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public PatientResponseDTO findById(Long id) {

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Paciente não encontrado com o ID: " + id
                        )
                );

        return toResponseDTO(patient);
    }

    private PatientResponseDTO toResponseDTO(
            Patient patient
    ) {

        User user = patient.getUser();

        return new PatientResponseDTO(
                patient.getId(),
                user.getName(),
                user.getEmail(),
                patient.getPhone(),
                patient.getBirthDate(),
                user.getActive()
        );
    }
}   