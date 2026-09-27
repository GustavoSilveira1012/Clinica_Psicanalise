package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.AuthActionToken;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.AuthActionTokenType;
import com.psicogest.psicogest.repository.AuthActionTokenRepository;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.security.auth.refresh.SecurityTokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** Issues and atomically consumes one-time auth tokens; raw tokens are returned only to the mail-delivery boundary. */
@Service
public class AuthActionTokenService {
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(20);
    private static final Duration EMAIL_VERIFICATION_TTL = Duration.ofHours(24);

    private final AuthActionTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final SecurityTokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionService sessions;
    private final Clock clock;

    public AuthActionTokenService(
            AuthActionTokenRepository tokenRepository,
            UserRepository userRepository,
            SecurityTokenGenerator tokenGenerator,
            PasswordEncoder passwordEncoder,
            UserSessionService sessions,
            Clock clock
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.tokenGenerator = tokenGenerator;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional
    public String issue(Long userId, AuthActionTokenType type) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Conta indisponível"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("Conta indisponível");
        }

        tokenRepository.invalidatePending(userId, type, now);
        String rawToken = tokenGenerator.generate();
        Duration ttl = switch (type) {
            case PASSWORD_RESET -> PASSWORD_RESET_TTL;
            case EMAIL_VERIFICATION -> EMAIL_VERIFICATION_TTL;
        };
        tokenRepository.save(AuthActionToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(tokenGenerator.hash(rawToken))
                .tokenType(type)
                .securityVersion(user.getSecurityVersion())
                .expiresAt(now.plus(ttl))
                .createdAt(now)
                .build());
        return rawToken;
    }

    @Transactional
    public boolean consume(
            String rawToken,
            AuthActionTokenType expectedType,
            Consumer<User> transactionalCompletion
    ) {
        if (transactionalCompletion == null) {
            throw new IllegalArgumentException("A conclusão transacional é obrigatória");
        }
        if (rawToken == null || !rawToken.matches("[A-Za-z0-9_-]{43}")) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String tokenHash = tokenGenerator.hash(rawToken);
        Long userId = tokenRepository.findUserIdByTokenHash(tokenHash).orElse(null);
        if (userId == null) {
            return false;
        }
        User lockedUser = userRepository.findByIdForUpdate(userId).orElse(null);
        if (lockedUser == null || !Boolean.TRUE.equals(lockedUser.getActive())) {
            return false;
        }
        AuthActionToken token = tokenRepository.findByTokenHashForUpdate(tokenHash).orElse(null);
        if (token == null
                || !token.getUser().getId().equals(userId)
                || token.getTokenType() != expectedType
                || token.getConsumedAt() != null
                || !token.getExpiresAt().isAfter(now)
                || !token.getSecurityVersion().equals(lockedUser.getSecurityVersion())) {
            return false;
        }

        token.setConsumedAt(now);
        transactionalCompletion.accept(lockedUser);
        return true;
    }

    /**
     * Completes a password reset atomically with token consumption and revokes
     * every existing session. Passwords are bounded to BCrypt's 72-byte input.
     */
    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        if (!isAcceptablePassword(newPassword)) {
            return false;
        }

        return consume(rawToken, AuthActionTokenType.PASSWORD_RESET, user -> {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setPasswordChangedAt(LocalDateTime.now(clock));
            user.setSecurityVersion(user.getSecurityVersion() + 1);
            user.setFailedLoginAttempts(0);
            user.setLastFailedLoginAt(null);
            user.setLockedUntil(null);
            user.setRequirePasswordChange(false);
            sessions.revokeAll(user.getId(), "PASSWORD_RESET");
        });
    }

    /** Confirms the account email only when a valid, unexpired one-time token is consumed. */
    @Transactional
    public boolean verifyEmail(String rawToken) {
        return consume(rawToken, AuthActionTokenType.EMAIL_VERIFICATION, user -> {
            if (user.getEmailVerifiedAt() == null) {
                user.setEmailVerifiedAt(LocalDateTime.now(clock));
            }
        });
    }

    private boolean isAcceptablePassword(String password) {
        if (password == null || password.length() > 72) {
            return false;
        }
        int codePoints = password.codePointCount(0, password.length());
        return codePoints >= 12
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
