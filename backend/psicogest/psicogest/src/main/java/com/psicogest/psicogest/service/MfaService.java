package com.psicogest.psicogest.service;

import com.psicogest.psicogest.exception.InvalidMfaException;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.*;
import com.psicogest.psicogest.repository.*;
import com.psicogest.psicogest.security.mfa.*;
import com.psicogest.psicogest.security.refresh.SecurityTokenGenerator;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
@Transactional(noRollbackFor = {InvalidMfaException.class, BadCredentialsException.class})
public class MfaService {
    private final ChallengeService challenges;
    private final MfaMethodRepository methods;
    private final MfaRecoveryCodeRepository recovery;
    private final UserRepository users;
    private final TotpService totp;
    private final MfaSecretCipher cipher;
    private final MfaProperties properties;
    private final SecurityTokenGenerator generator;
    private final AuthTokenService tokens;
    private final UserSessionService sessions;
    private final PasswordEncoder passwords;
    private final Clock clock;
    public MfaService(ChallengeService challenges, MfaMethodRepository methods, MfaRecoveryCodeRepository recovery,
            UserRepository users, TotpService totp, MfaSecretCipher cipher, MfaProperties properties,
            SecurityTokenGenerator generator, AuthTokenService tokens, UserSessionService sessions,
            PasswordEncoder passwords, Clock clock) {
        this.challenges = challenges; this.methods = methods; this.recovery = recovery; this.users = users;
        this.totp = totp; this.cipher = cipher; this.properties = properties; this.generator = generator;
        this.tokens = tokens; this.sessions = sessions; this.passwords = passwords; this.clock = clock;
    }

    // Optional enrollment for patients also requires the password again, not only a bearer token.
    public String enrollment(Long userId, String password, String ip, String userAgent) {
        User user = users.findByIdForUpdate(userId).orElseThrow(InvalidMfaException::new);
        if (password == null || !passwords.matches(password, user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            user.setLastFailedLoginAt(now());
            throw new BadCredentialsException("Credenciais inválidas");
        }
        if (!Boolean.TRUE.equals(user.getActive())
                || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now()))
                || methods.existsByUserIdAndStatus(userId, MfaMethodStatus.ACTIVE)) {
            throw new InvalidMfaException();
        }
        return challenges.issue(user, AuthenticationChallengeType.MFA_ENROLLMENT_REQUIRED, ip, userAgent);
    }

    public SetupResult setup(String raw) {
        var challenge = challenges.require(raw, AuthenticationChallengeType.MFA_ENROLLMENT_REQUIRED);
        User user = challenge.getUser();
        if (methods.existsByUserIdAndStatus(user.getId(), MfaMethodStatus.ACTIVE)) throw new InvalidMfaException();
        var pending = methods.findByUserIdAndStatus(user.getId(), MfaMethodStatus.PENDING);
        if (pending.isPresent()) {
            if (challenge.getId().equals(pending.get().getEnrollmentChallengeId())) throw new InvalidMfaException();
            revokeMethod(pending.get());
            methods.flush(); // Release the unique pending-method slot before inserting its replacement.
        }
        String secret = totp.generateSecret();
        var encrypted = cipher.encrypt(secret);
        methods.saveAndFlush(MfaMethod.builder().id(UUID.randomUUID()).user(user).methodType(MfaMethodType.TOTP)
                .status(MfaMethodStatus.PENDING).label(properties.issuer()).createdAt(now())
                .enrollmentChallengeId(challenge.getId()).secretCiphertext(encrypted.ciphertext())
                .secretIv(encrypted.iv()).build());
        String uri = "otpauth://totp/" + encode(properties.issuer()) + ":" + encode(user.getEmail())
                + "?secret=" + secret + "&issuer=" + encode(properties.issuer()) + "&algorithm=SHA1&digits=6&period=30";
        return new SetupResult(secret, uri);
    }

    public EnrollmentResult confirm(String raw, String code, String ip, String userAgent) {
        var challenge = challenges.require(raw, AuthenticationChallengeType.MFA_ENROLLMENT_REQUIRED);
        User user = challenge.getUser();
        MfaMethod method = methods.findByUserIdAndStatus(user.getId(), MfaMethodStatus.PENDING)
                .orElseThrow(InvalidMfaException::new);
        if (!challenge.getId().equals(method.getEnrollmentChallengeId())) challenges.reject(challenge);
        acceptTotp(challenge, method, code);
        method.setStatus(MfaMethodStatus.ACTIVE);
        method.setVerifiedAt(now());
        challenges.consume(challenge);
        // Activating a factor invalidates earlier password-only sessions and challenges.
        user.setSecurityVersion(user.getSecurityVersion() + 1);
        sessions.revokeAll(user.getId(), "MFA_ACTIVATED");
        recovery.invalidateForUser(user.getId(), now());
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String recoveryCode = generator.generate();
            codes.add(recoveryCode);
            recovery.save(MfaRecoveryCode.builder().id(UUID.randomUUID()).user(user)
                    .codeHash(generator.hash(recoveryCode)).createdAt(now()).build());
        }
        return new EnrollmentResult(tokens.authenticate(user, ip, userAgent), List.copyOf(codes));
    }

    public AuthService.AuthTokens verify(String raw, String code, String ip, String userAgent) {
        var challenge = challenges.require(raw, AuthenticationChallengeType.MFA_REQUIRED);
        acceptTotp(challenge, activeMethod(challenge.getUser()), code);
        challenges.consume(challenge);
        return tokens.authenticate(challenge.getUser(), ip, userAgent);
    }

    public AuthService.AuthTokens recover(String raw, String code, String ip, String userAgent) {
        var challenge = challenges.require(raw, AuthenticationChallengeType.MFA_REQUIRED);
        activeMethod(challenge.getUser());
        var recoveryCode = code == null ? Optional.<MfaRecoveryCode>empty()
                : recovery.findByUserIdAndCodeHashAndUsedAtIsNull(challenge.getUser().getId(), generator.hash(code));
        if (recoveryCode.isEmpty()) challenges.reject(challenge);
        recoveryCode.orElseThrow().setUsedAt(now());
        challenges.consume(challenge);
        return tokens.authenticate(challenge.getUser(), ip, userAgent);
    }

    // A fresh login challenge proves password re-entry; the current TOTP proves the existing factor.
    public void remove(Long userId, String raw, String code) {
        var challenge = challenges.require(raw, AuthenticationChallengeType.MFA_REQUIRED);
        if (!challenge.getUser().getId().equals(userId)) throw new InvalidMfaException();
        MfaMethod method = activeMethod(challenge.getUser());
        acceptTotp(challenge, method, code);
        challenges.consume(challenge);
        revokeMethod(method);
        recovery.invalidateForUser(userId, now());
        challenge.getUser().setSecurityVersion(challenge.getUser().getSecurityVersion() + 1);
        sessions.revokeAll(userId, "MFA_REMOVED");
    }

    private MfaMethod activeMethod(User user) {
        return methods.findByUserIdAndStatus(user.getId(), MfaMethodStatus.ACTIVE).orElseThrow(InvalidMfaException::new);
    }

    private void acceptTotp(AuthenticationChallenge challenge, MfaMethod method, String code) {
        String secret = cipher.decrypt(method.getSecretCiphertext(), method.getSecretIv());
        var step = totp.verifyAndReturnStep(secret, code, method.getLastAcceptedTimeStep());
        if (step.isEmpty()) challenges.reject(challenge);
        method.setLastAcceptedTimeStep(step.orElseThrow());
        method.setLastUsedAt(now());
    }

    private void revokeMethod(MfaMethod method) {
        method.setStatus(MfaMethodStatus.REVOKED);
        method.setRevokedAt(now());
        method.setSecretCiphertext(null);
        method.setSecretIv(null);
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    public record SetupResult(String secret, String otpauthUri) {}
    public record EnrollmentResult(AuthService.AuthTokens tokens, List<String> recoveryCodes) {}
}
