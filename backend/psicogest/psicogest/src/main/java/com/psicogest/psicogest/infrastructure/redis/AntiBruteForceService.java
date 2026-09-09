package com.psicogest.psicogest.infrastructure.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AntiBruteForceService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String LOGIN_ATTEMPT_PREFIX = "login_attempts:";
    private static final String ACCOUNT_LOCKED_PREFIX = "account_locked:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    public AntiBruteForceService(
            RedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAccountLocked(String identifier) {
        String lockedKey = ACCOUNT_LOCKED_PREFIX + identifier;
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(lockedKey)
        );
    }

    public void recordFailedAttempt(String identifier) {
        String attemptKey = LOGIN_ATTEMPT_PREFIX + identifier;

        redisTemplate.opsForValue().increment(attemptKey);
        redisTemplate.expire(attemptKey, ATTEMPT_WINDOW);

        Long attempts = Long.parseLong(
                redisTemplate.opsForValue().get(attemptKey)
        );

        if (attempts >= MAX_ATTEMPTS) {
            lockAccount(identifier);
        }
    }

    public void clearAttempts(String identifier) {
        String attemptKey = LOGIN_ATTEMPT_PREFIX + identifier;
        redisTemplate.delete(attemptKey);
    }

    public long getAttemptCount(String identifier) {
        String attemptKey = LOGIN_ATTEMPT_PREFIX + identifier;
        String value = redisTemplate.opsForValue().get(attemptKey);
        return value != null ? Long.parseLong(value) : 0L;
    }

    private void lockAccount(String identifier) {
        String lockedKey = ACCOUNT_LOCKED_PREFIX + identifier;
        redisTemplate.opsForValue().set(
                lockedKey,
                "locked",
                LOCK_DURATION
        );
    }

    public void unlockAccount(String identifier) {
        String lockedKey = ACCOUNT_LOCKED_PREFIX + identifier;
        redisTemplate.delete(lockedKey);
    }
}
