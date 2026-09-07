package com.psicogest.psicogest.service;

import com.psicogest.psicogest.exception.*;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.repository.RefreshTokenRepository;
import com.psicogest.psicogest.security.jwt.JwtProperties;
import com.psicogest.psicogest.security.refresh.RefreshTokenGenerator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    private final RefreshTokenGenerator generator;

    private final JwtProperties properties;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            RefreshTokenGenerator generator,
            JwtProperties properties) {

        this.repository = repository;

        this.generator = generator;

        this.properties = properties;
    }

    @Transactional
    public IssuedRefreshToken issueInitial(
            User user,
            String ip,
            String userAgent) {

        UUID familyId = UUID.randomUUID();

        return issue(
                user,
                familyId,
                ip,
                userAgent);
    }

    private IssuedRefreshToken issue(
            User user,
            UUID familyId,
            String ip,
            String userAgent) {

        String raw = generator.generate();

        LocalDateTime now = LocalDateTime.now();

        RefreshToken entity = RefreshToken.builder()

                .id(
                        UUID.randomUUID())

                .user(user)

                .familyId(
                        familyId)

                .tokenHash(
                        generator.hash(raw))

                .securityVersion(
                        user.getSecurityVersion())

                .issuedAt(now)

                .expiresAt(
                        now.plus(
                                properties
                                        .refreshTokenTtl()))

                .createdIp(ip)

                .userAgentHash(
                        userAgent != null
                                ? generator.hash(
                                        userAgent)
                                : null)

                .build();

        repository.saveAndFlush(
                entity);

        return new IssuedRefreshToken(
                raw,
                entity);
    }

    public record IssuedRefreshToken(

            String rawToken,

            RefreshToken entity

    ) {
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = RefreshTokenReuseDetectedException.class
    )
    public RotationResult rotate(
            String rawToken,
            String ip,
            String userAgent) {

        String hash = generator.hash(
                rawToken);

        RefreshToken current = repository
                .findByTokenHashForUpdate(
                        hash)
                .orElseThrow(
                        InvalidRefreshTokenException::new);

        LocalDateTime now = LocalDateTime.now();

        /*
         * REUSE DETECTION
         */
        if (current.getConsumedAt() != null) {

            repository.revokeFamily(
                    current.getFamilyId(),
                    now,
                    "REFRESH_TOKEN_REUSE");

            /*
             * Nunca logar rawToken.
             *
             * SecurityEvent será integrado
             * no próximo bloco.
             */

            throw new RefreshTokenReuseDetectedException();
        }

        if (current.getRevokedAt() != null) {

            throw new InvalidRefreshTokenException();
        }

        if (!now.isBefore(
                current.getExpiresAt())) {

            throw new InvalidRefreshTokenException();
        }

        User user = current.getUser();

        if (Boolean.FALSE.equals(
                user.getActive())) {

            repository.revokeFamily(
                    current.getFamilyId(),
                    now,
                    "USER_INACTIVE");

            throw new InvalidRefreshTokenException();
        }

        if (user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(now)) {
            repository.revokeFamily(
                    current.getFamilyId(),
                    now,
                    "USER_LOCKED");

            throw new InvalidRefreshTokenException();
        }

        if (!current.getSecurityVersion()
                .equals(
                        user.getSecurityVersion())) {

            repository.revokeFamily(
                    current.getFamilyId(),
                    now,
                    "SECURITY_VERSION_CHANGED");

            throw new InvalidRefreshTokenException();
        }

        IssuedRefreshToken replacement = issue(
                user,
                current.getFamilyId(),
                ip,
                userAgent);

        current.setConsumedAt(now);

        current.setReplacedBy(
                replacement.entity());

        repository.saveAndFlush(
                current);

        return new RotationResult(
                user,
                replacement.rawToken());
    }

    @Transactional
    public void revokeCurrentSession(String rawToken) {
        String hash = generator.hash(rawToken);

        repository.findByTokenHashForUpdate(hash)
                .ifPresent(token -> repository.revokeFamily(
                        token.getFamilyId(),
                        LocalDateTime.now(),
                        "LOGOUT"));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(
                userId,
                LocalDateTime.now(),
                "LOGOUT_ALL_DEVICES");
    }

    public record RotationResult(

            User user,

            String refreshToken

    ) {
    }
}
