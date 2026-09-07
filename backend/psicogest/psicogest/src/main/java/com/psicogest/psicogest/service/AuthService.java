package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.auth.*;
import com.psicogest.psicogest.exception.RefreshTokenReuseDetectedException;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.*;
import com.psicogest.psicogest.repository.*;
import com.psicogest.psicogest.security.jwt.JwtService;
import com.psicogest.psicogest.security.mfa.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final MfaMethodRepository methods;
    private final ChallengeService challenges;
    private final AuthTokenService tokens;
    private final RefreshTokenService refreshTokens;
    private final UserSessionService sessions;
    private final JwtService jwt;
    private final Clock clock;
    private final String dummyHash;
    public AuthService(UserRepository users, PasswordEncoder passwords, MfaMethodRepository methods,
            ChallengeService challenges, AuthTokenService tokens, RefreshTokenService refreshTokens,
            UserSessionService sessions, JwtService jwt, Clock clock) {
        this.users = users; this.passwords = passwords; this.methods = methods; this.challenges = challenges;
        this.tokens = tokens; this.refreshTokens = refreshTokens; this.sessions = sessions;
        this.jwt = jwt; this.clock = clock; this.dummyHash = passwords.encode("dummy-password-never-used");
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public LoginResult login(LoginRequest dto, String ip, String userAgent) {
        User user = users.findByEmailForUpdate(dto.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
        boolean valid = passwords.matches(dto.password(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !valid) {
            if (user != null) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                user.setLastFailedLoginAt(LocalDateTime.now(clock));
            }
            throw new BadCredentialsException("Credenciais inválidas");
        }
        if (!Boolean.TRUE.equals(user.getActive()) || (user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(LocalDateTime.now(clock)))) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        boolean activeMfa = methods.existsByUserIdAndStatus(user.getId(), MfaMethodStatus.ACTIVE);
        if (activeMfa || MfaPolicyService.requiresMfa(user)) {
            var type = activeMfa ? AuthenticationChallengeType.MFA_REQUIRED
                    : AuthenticationChallengeType.MFA_ENROLLMENT_REQUIRED;
            String challenge = challenges.issue(user, type, ip, userAgent);
            return new LoginResult(new LoginResponse(LoginStatus.valueOf(type.name()), null, challenge), null);
        }
        AuthTokens authenticated = tokens.authenticate(user, ip, userAgent);
        return new LoginResult(new LoginResponse(LoginStatus.AUTHENTICATED, authenticated.response(), null),
                authenticated.refreshToken());
    }

    @Transactional(noRollbackFor = RefreshTokenReuseDetectedException.class)
    public AuthTokens refresh(String raw, String ip, String userAgent) {
        var rotation = refreshTokens.rotate(raw, ip, userAgent);
        return new AuthTokens(tokens.response(rotation.user(), jwt.issueAccessToken(rotation.user(), rotation.sessionId())),
                rotation.refreshToken());
    }

    @Transactional
    public void logoutAll(Long userId) {
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        user.setSecurityVersion(user.getSecurityVersion() + 1);
        sessions.revokeAll(userId, "LOGOUT_ALL_DEVICES");
    }

    public record AuthTokens(AuthResponse response, String refreshToken) {}
    public record LoginResult(LoginResponse response, String refreshToken) {}
}
