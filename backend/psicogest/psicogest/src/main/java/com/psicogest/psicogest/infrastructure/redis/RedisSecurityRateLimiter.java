package com.psicogest.psicogest.infrastructure.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import com.psicogest.psicogest.exception.SecurityInfrastructureException;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class RedisSecurityRateLimiter {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> rateLimitScript;

    private static final String LUA_RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = tonumber(redis.call('GET', key) or 0)

            if current < limit then
                redis.call('INCR', key)
                redis.call('EXPIRE', key, window)
                return 1
            else
                return 0
            end
            """;

    private static final String LUA_TOKEN_BUCKET_SCRIPT = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_period_ms = tonumber(ARGV[2])
            local clock = redis.call('TIME')
            local now_ms = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
            local state = redis.call('HMGET', key, 'tokens', 'updated_at_ms')
            local tokens = tonumber(state[1])
            local updated_at_ms = tonumber(state[2])

            if tokens == nil or updated_at_ms == nil then
                tokens = capacity
                updated_at_ms = now_ms
            end

            local elapsed_ms = math.max(0, now_ms - updated_at_ms)
            tokens = math.min(capacity, tokens + elapsed_ms * capacity / refill_period_ms)
            local allowed = 0
            if tokens >= 1 then
                tokens = tokens - 1
                allowed = 1
            end

            redis.call('HSET', key, 'tokens', tokens, 'updated_at_ms', now_ms)
            redis.call('PEXPIRE', key, refill_period_ms * 2)
            return allowed
            """;

    public RedisSecurityRateLimiter(
            RedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = RedisScript.of(
                LUA_RATE_LIMIT_SCRIPT,
                Long.class
        );
    }

    public boolean tryConsume(
            String key,
            int limit,
            Duration window
    ) {
        try {
            List<String> keys = Collections.singletonList(key);
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    keys,
                    String.valueOf(limit),
                    String.valueOf(window.getSeconds())
            );

            return result != null && result == 1L;
        } catch (Exception e) {
            throw new SecurityInfrastructureException(
                    "Falha ao verificar rate limit",
                    e
            );
        }
    }

    /** Replenishes tokens continuously over the duration needed to refill capacity. */
    public boolean tryConsumeTokenBucket(String key, int capacity, Duration refillPeriod) {
        if (key == null || key.isBlank() || capacity < 1 || refillPeriod == null
                || refillPeriod.isNegative() || refillPeriod.isZero()
                || refillPeriod.toMillis() < 1) {
            throw new IllegalArgumentException("Parâmetros do token bucket inválidos");
        }
        try {
            Long result = redisTemplate.execute(
                    RedisScript.of(LUA_TOKEN_BUCKET_SCRIPT, Long.class),
                    Collections.singletonList(key),
                    String.valueOf(capacity),
                    String.valueOf(refillPeriod.toMillis())
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            throw new SecurityInfrastructureException("Falha ao verificar rate limit outbound", e);
        }
    }

    public long getCurrentCount(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0L;
        } catch (Exception e) {
            throw new SecurityInfrastructureException(
                    "Falha ao obter contagem de rate limit",
                    e
            );
        }
    }

    public void reset(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            throw new SecurityInfrastructureException(
                    "Falha ao resetar rate limit",
                    e
            );
        }
    }
}
