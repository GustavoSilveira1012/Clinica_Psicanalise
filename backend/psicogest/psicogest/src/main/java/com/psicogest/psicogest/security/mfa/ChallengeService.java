package com.psicogest.psicogest.security.mfa;

import com.psicogest.psicogest.exception.InvalidMfaException;
import com.psicogest.psicogest.model.entity.*;
import com.psicogest.psicogest.model.enums.AuthenticationChallengeType;
import com.psicogest.psicogest.repository.*;
import com.psicogest.psicogest.security.refresh.SecurityTokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@Transactional(noRollbackFor = InvalidMfaException.class)
public class ChallengeService {
    private final AuthenticationChallengeRepository challenges;
    private final UserRepository users;
    private final SecurityTokenGenerator tokens;
    private final MfaProperties properties;
    private final Clock clock;
    public ChallengeService(AuthenticationChallengeRepository challenges, UserRepository users,
            SecurityTokenGenerator tokens, MfaProperties properties, Clock clock) {
        this.challenges = challenges; this.users = users; this.tokens = tokens;
        this.properties = properties; this.clock = clock;
    }

    public String issue(User user, AuthenticationChallengeType type, String ip, String userAgent) {
        String raw = tokens.generate();
        LocalDateTime now = LocalDateTime.now(clock);
        challenges.saveAndFlush(AuthenticationChallenge.builder().id(UUID.randomUUID()).user(user)
                .tokenHash(tokens.hash(raw)).challengeType(type).securityVersion(user.getSecurityVersion())
                .expiresAt(now.plus(properties.challengeTtl())).createdAt(now).createdIp(ip)
                .userAgentHash(userAgent == null ? null : tokens.hash(userAgent)).build());
        return raw;
    }

    // User row is always locked first: serializes OTP/recovery consumption across challenges.
    public AuthenticationChallenge require(String raw, AuthenticationChallengeType type) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) throw new InvalidMfaException();
        String hash = tokens.hash(raw);
        Long userId = challenges.findUserIdByHash(hash).orElseThrow(InvalidMfaException::new);
        User user = users.findByIdForUpdate(userId).orElseThrow(InvalidMfaException::new);
        AuthenticationChallenge c = challenges.findByTokenHashForUpdate(hash).orElseThrow(InvalidMfaException::new);
        LocalDateTime now = LocalDateTime.now(clock);
        if (c.getChallengeType() != type || c.getConsumedAt() != null || !now.isBefore(c.getExpiresAt())
                || c.getAttemptCount() >= properties.maxAttempts()
                || !Objects.equals(c.getSecurityVersion(), user.getSecurityVersion())
                || !Boolean.TRUE.equals(user.getActive())
                || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))) {
            throw new InvalidMfaException();
        }
        return c;
    }

    public void reject(AuthenticationChallenge c) {
        c.setAttemptCount(c.getAttemptCount() + 1);
        if (c.getAttemptCount() >= properties.maxAttempts()) c.setConsumedAt(LocalDateTime.now(clock));
        challenges.saveAndFlush(c);
        // Both this transaction and its caller must commit the attempt counter on rejection.
        throw new InvalidMfaException();
    }

    public void consume(AuthenticationChallenge c) { c.setConsumedAt(LocalDateTime.now(clock)); }
}
