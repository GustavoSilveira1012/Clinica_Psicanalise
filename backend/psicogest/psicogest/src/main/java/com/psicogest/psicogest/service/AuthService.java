package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.auth.AuthResponse;
import com.psicogest.psicogest.dto.auth.LoginRequest;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.security.jwt.JwtProperties;
import com.psicogest.psicogest.security.jwt.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final String dummyHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
        this.dummyHash = passwordEncoder.encode("dummy-password-never-used");
    }

    @Transactional
    public AuthTokens login(
            LoginRequest dto,
            String ip,
            String userAgent
    ) {
        String normalizedEmail = dto.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElse(null);

        String passwordHash = user != null
                ? user.getPasswordHash()
                : dummyHash;

        boolean valid = passwordEncoder.matches(dto.password(), passwordHash);

        if (user == null || !valid) {
            if (user != null) {
                Integer failedAttempts = user.getFailedLoginAttempts();
                user.setFailedLoginAttempts(
                        (failedAttempts == null ? 0 : failedAttempts) + 1);
                user.setLastFailedLoginAt(LocalDateTime.now());
                userRepository.save(user);
            }

            throw invalidCredentials();
        }

        if (Boolean.FALSE.equals(user.getActive())
                || (user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(LocalDateTime.now()))) {
            throw invalidCredentials();
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        JwtService.AccessToken accessToken = jwtService.issueAccessToken(user);
        RefreshTokenService.IssuedRefreshToken refresh =
                refreshTokenService.issueInitial(user, ip, userAgent);

        return new AuthTokens(
                toAuthResponse(user, accessToken),
                refresh.rawToken());
    }

    @Transactional
    public AuthTokens refresh(
            String rawRefreshToken,
            String ip,
            String userAgent
    ) {
        RefreshTokenService.RotationResult rotation =
                refreshTokenService.rotate(rawRefreshToken, ip, userAgent);
        JwtService.AccessToken accessToken =
                jwtService.issueAccessToken(rotation.user());

        return new AuthTokens(
                toAuthResponse(rotation.user(), accessToken),
                rotation.refreshToken());
    }

    @Transactional
    public void logoutAll(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        Integer securityVersion = user.getSecurityVersion();
        user.setSecurityVersion((securityVersion == null ? 0 : securityVersion) + 1);
        userRepository.saveAndFlush(user);
        refreshTokenService.revokeAllForUser(userId);
    }

    private AuthResponse toAuthResponse(
            User user,
            JwtService.AccessToken accessToken
    ) {
        long expiresIn = Math.max(
                0,
                Duration.between(Instant.now(), accessToken.expiresAt()).toSeconds());

        return new AuthResponse(
                accessToken.value(),
                "Bearer",
                expiresIn,
                user.getId(),
                user.getRole().name());
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Credenciais inválidas");
    }

    public record AuthTokens(
            AuthResponse response,
            String refreshToken
    ) {
    }
}
