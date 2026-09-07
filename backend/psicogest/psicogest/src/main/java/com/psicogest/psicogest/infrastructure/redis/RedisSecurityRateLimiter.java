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
