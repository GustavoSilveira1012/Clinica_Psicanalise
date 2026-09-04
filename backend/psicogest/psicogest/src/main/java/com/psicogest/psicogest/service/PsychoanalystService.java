package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.psychoanalyst.PsychoanalystCreateDTO;
import com.psicogest.psicogest.dto.psychoanalyst.PsychoanalystResponseDTO;
import com.psicogest.psicogest.exception.EmailAlreadyExistsException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.Psychoanalyst;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.UserRole;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import com.psicogest.psicogest.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PsychoanalystService {

    private final PsychoanalystRepository psychoanalystRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PsychoanalystService(
            PsychoanalystRepository psychoanalystRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.psychoanalystRepository = psychoanalystRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PsychoanalystResponseDTO create(
            PsychoanalystCreateDTO dto
    ) {

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
                .role(UserRole.PSYCHOANALYST)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        Psychoanalyst psychoanalyst =
                Psychoanalyst.builder()
                        .user(savedUser)
                        .licenseNumber(dto.licenseNumber())
                        .specialization(dto.specialization())
                        .bio(dto.bio())
                        .phone(dto.phone())
                        .build();

        Psychoanalyst savedPsychoanalyst =
                psychoanalystRepository.save(psychoanalyst);

        return toResponseDTO(savedPsychoanalyst);
    }

    public List<PsychoanalystResponseDTO> findAll() {

        return psychoanalystRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public PsychoanalystResponseDTO findById(Long id) {

        Psychoanalyst psychoanalyst =
                psychoanalystRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Psicanalista não encontrado com o ID: "
                                                + id
                                )
                        );

        return toResponseDTO(psychoanalyst);
    }

    private PsychoanalystResponseDTO toResponseDTO(
            Psychoanalyst psychoanalyst
    ) {

        User user = psychoanalyst.getUser();

        return new PsychoanalystResponseDTO(
                psychoanalyst.getId(),
                user.getName(),
                user.getEmail(),
                psychoanalyst.getLicenseNumber(),
                psychoanalyst.getSpecialization(),
                psychoanalyst.getBio(),
                psychoanalyst.getPhone(),
                user.getActive()
        );
    }
}