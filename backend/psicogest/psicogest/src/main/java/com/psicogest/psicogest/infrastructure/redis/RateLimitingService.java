package com.psicogest.psicogest.infrastructure.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class RateLimitingService {

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

    public RateLimitingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = RedisScript.of(
                LUA_RATE_LIMIT_SCRIPT,
                Long.class
        );
    }

    public boolean allowRequest(
            String key,
            int limit,
            Duration window
    ) {
        List<String> keys = Collections.singletonList(key);
        Long result = redisTemplate.execute(
                rateLimitScript,
                keys,
                String.valueOf(limit),
                String.valueOf(window.getSeconds())
        );

        return result != null && result == 1L;
    }

    public long getCurrentCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    public void resetKey(String key) {
        redisTemplate.delete(key);
    }
}
