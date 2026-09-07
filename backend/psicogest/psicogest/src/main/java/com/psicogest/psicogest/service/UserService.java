package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.user.UserCreateDTO;
import com.psicogest.psicogest.dto.user.UserResponseDTO;
import com.psicogest.psicogest.dto.user.UserUpdateDTO;
import com.psicogest.psicogest.exception.EmailAlreadyExistsException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponseDTO> findAll() {

        return userRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public UserResponseDTO findById(Long id) {

        User user = findUserById(id);

        return toResponseDTO(user);
    }

    public UserResponseDTO create(UserCreateDTO dto) {

        if (userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException(
                    "Já existe um usuário cadastrado com este e-mail");
        }

        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .passwordHash(passwordEncoder.encode(dto.password()))
                .role(dto.role())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        return toResponseDTO(savedUser);
    }

    private User findUserById(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não encontrado com o ID: " + id));
    }

    private UserResponseDTO toResponseDTO(User user) {

        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getActive(),
                user.getSecurityVersion(),
                user.getFailedLoginAttempts(),
                user.getLastFailedLoginAt(),
                user.getLockedUntil(),
                user.getLastLoginAt(),
                user.getPasswordChangedAt(),
                user.getRequirePasswordChange());
    }

    public UserResponseDTO update(
            Long id,
            UserUpdateDTO dto) {

        User user = findUserById(id);

        if (!user.getEmail().equals(dto.email())
                && userRepository.existsByEmail(dto.email())) {

            throw new EmailAlreadyExistsException(
                    "Já existe um usuário cadastrado com este e-mail");
        }

        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setRole(dto.role());
        user.setActive(dto.active());

        if (dto.password() != null
                && !dto.password().isBlank()) {

            user.setPasswordHash(
                    passwordEncoder.encode(dto.password()));

            user.setPasswordChangedAt(LocalDateTime.now());
            user.setSecurityVersion(
                    user.getSecurityVersion() + 1);
        }

        User updatedUser = userRepository.save(user);

        return toResponseDTO(updatedUser);
    }

    public void delete(Long id) {

        User user = findUserById(id);

        userRepository.delete(user);
    }

}
