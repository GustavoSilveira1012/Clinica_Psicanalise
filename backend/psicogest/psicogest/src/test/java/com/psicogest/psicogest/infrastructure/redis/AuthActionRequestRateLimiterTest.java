package com.psicogest.psicogest.infrastructure.redis;

import com.psicogest.psicogest.config.AuthActionMailProperties;
import com.psicogest.psicogest.model.enums.AuthActionTokenType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthActionRequestRateLimiterTest {

    private final RedisSecurityRateLimiter redis = mock(RedisSecurityRateLimiter.class);
    private final AuthActionMailProperties properties = new AuthActionMailProperties(
            true, "no-reply@example.invalid", "https://app.example.invalid",
            Base64.getEncoder().encodeToString(new byte[32]), 10, 3, Duration.ofHours(1));

    @Test
    void hashesIpAndEmailWithHmacBeforeUsingRedisKeys() {
        when(redis.tryConsumeTokenBucket(any(), anyInt(), any())).thenReturn(true);
        AuthActionRequestRateLimiter limiter = new AuthActionRequestRateLimiter(redis, properties);

        assertThat(limiter.allow("192.0.2.44", "person@example.invalid", AuthActionTokenType.PASSWORD_RESET)).isTrue();

        verify(redis).tryConsumeTokenBucket(argThat(key -> key.startsWith("auth-action:ip:")
                && !key.contains("192.0.2.44")), org.mockito.ArgumentMatchers.eq(10), org.mockito.ArgumentMatchers.eq(Duration.ofHours(1)));
        verify(redis).tryConsumeTokenBucket(argThat(key -> key.startsWith("auth-action:address:")
                && !key.contains("person@example.invalid")), org.mockito.ArgumentMatchers.eq(3), org.mockito.ArgumentMatchers.eq(Duration.ofHours(1)));
    }

    @Test
    void deniesTheAccountBucketWhenTheIpBucketAllowsButTheAccountLimitIsReached() {
        when(redis.tryConsumeTokenBucket(any(), anyInt(), any())).thenReturn(true, false);
        AuthActionRequestRateLimiter limiter = new AuthActionRequestRateLimiter(redis, properties);

        assertThat(limiter.allow("192.0.2.44", "person@example.invalid", AuthActionTokenType.EMAIL_VERIFICATION)).isFalse();
    }
}
