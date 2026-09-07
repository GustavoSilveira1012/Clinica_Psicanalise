package com.psicogest.psicogest.security.rate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component 
public class RedisSecurityRateLimiter {
    private static final String LUA = """
            local key = KEYS[1]

            local capacity =
                tonumber(ARGV[1])

            local refill_per_second =
                tonumber(ARGV[2])

            local requested =
                tonumber(ARGV[3])

            local current_time =
                redis.call('TIME')

            local now =
                tonumber(current_time[1])
                +
                tonumber(current_time[2]) / 1000000

            local values =
                redis.call(
                    'HMGET',
                    key,
                    'tokens',
                    'timestamp'
                )

            local tokens =
                tonumber(values[1])

            local timestamp =
                tonumber(values[2])

            if tokens == nil then
                tokens = capacity
            end

            if timestamp == nil then
                timestamp = now
            end

            local elapsed =
                math.max(
                    0,
                    now - timestamp
                )

            tokens =
                math.min(
                    capacity,
                    tokens
                    +
                    elapsed * refill_per_second
                )

            local allowed = 0

            if tokens >= requested then

                tokens =
                    tokens - requested

                allowed = 1
            end

            redis.call(
                'HSET',
                key,
                'tokens',
                tokens,
                'timestamp',
                now
            )

            local ttl =
                math.ceil(
                    (capacity / refill_per_second) * 2
                )

            redis.call(
                'EXPIRE',
                key,
                ttl
            )

            return allowed
            """;

    private static final DefaultRedisScript<Long>
            RATE_LIMIT_SCRIPT =
            new DefaultRedisScript<>(
                    LUA,
                    Long.class
            );

    private final StringRedisTemplate redis;

    public RedisSecurityRateLimiter(
            StringRedisTemplate redis
    ) {

        this.redis = redis;
    }

    public boolean tryConsume(
            String key,
            int capacity,
            Duration refillPeriod
    ) {

        double seconds =
                refillPeriod.toMillis()
                        / 1000.0;

        double refillPerSecond =
                capacity / seconds;

        Long result =
                redis.execute(

                        RATE_LIMIT_SCRIPT,

                        List.of(key),

                        String.valueOf(
                                capacity
                        ),

                        String.valueOf(
                                refillPerSecond
                        ),

                        "1"
                );

        return Long.valueOf(1)
                .equals(result);
    }
}
