package com.psicogest.psicogest.infrastructure.redis;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class RedisSecurityRateLimiterIntegrationTest {

    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    private static LettuceConnectionFactory connectionFactory;
    private static RedisSecurityRateLimiter limiter;

    @BeforeAll
    static void startRedis() {
        REDIS.start();
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer strings = new StringRedisSerializer();
        template.setKeySerializer(strings);
        template.setValueSerializer(strings);
        template.setHashKeySerializer(strings);
        template.setHashValueSerializer(strings);
        template.afterPropertiesSet();
        limiter = new RedisSecurityRateLimiter(template);
    }

    @AfterAll
    static void stopRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        REDIS.stop();
    }

    @Test
    void refillsTokensGraduallyInsteadOfResettingOnlyAfterIdleWindow() throws Exception {
        String key = "test:notification:outbound:" + UUID.randomUUID();
        Duration fullRefillPeriod = Duration.ofSeconds(2);

        assertThat(limiter.tryConsumeTokenBucket(key, 2, fullRefillPeriod)).isTrue();
        assertThat(limiter.tryConsumeTokenBucket(key, 2, fullRefillPeriod)).isTrue();
        assertThat(limiter.tryConsumeTokenBucket(key, 2, fullRefillPeriod)).isFalse();

        Thread.sleep(1_100);
        assertThat(limiter.tryConsumeTokenBucket(key, 2, fullRefillPeriod)).isTrue();
        assertThat(limiter.tryConsumeTokenBucket(key, 2, fullRefillPeriod)).isFalse();

        limiter.reset(key);
    }

    @Test
    void concurrentRequestsCannotConsumeMoreThanAvailableCapacity() throws Exception {
        String key = "test:notification:outbound:" + UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(12);
        try {
            List<Callable<Boolean>> requests = new ArrayList<>();
            for (int index = 0; index < 40; index++) {
                requests.add(() -> limiter.tryConsumeTokenBucket(key, 5, Duration.ofHours(1)));
            }
            List<Future<Boolean>> results = executor.invokeAll(requests);
            long accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    accepted++;
                }
            }
            assertThat(accepted).isEqualTo(5);
        } finally {
            executor.shutdownNow();
            limiter.reset(key);
        }
    }
}
