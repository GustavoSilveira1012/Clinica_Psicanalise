package com.psicogest.psicogest.infrastructure.redis;

import com.psicogest.psicogest.config.NotificationOutboundRateLimitProperties;
import com.psicogest.psicogest.exception.RateLimitExceededException;
import com.psicogest.psicogest.exception.SecurityInfrastructureException;
import com.psicogest.psicogest.model.entity.SecurityEvent;
import com.psicogest.psicogest.model.enums.NotificationChannel;
import com.psicogest.psicogest.security.event.SecurityEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisNotificationOutboundRateLimiterTest {

    private final RedisSecurityRateLimiter redis = mock(RedisSecurityRateLimiter.class);
    private final SecurityEventService securityEvents = mock(SecurityEventService.class);
    private final NotificationOutboundRateLimitProperties limits = new NotificationOutboundRateLimitProperties(
            new NotificationOutboundRateLimitProperties.Bucket(1000, Duration.ofHours(1)),
            new NotificationOutboundRateLimitProperties.Bucket(500, Duration.ofHours(1)),
            new NotificationOutboundRateLimitProperties.Bucket(250, Duration.ofHours(1)));
    private final RedisNotificationOutboundRateLimiter limiter =
            new RedisNotificationOutboundRateLimiter(redis, limits, securityEvents);
    private final UUID financialEntityId = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    @BeforeEach
    void allowEmailQuota() {
        when(redis.tryConsumeTokenBucket(
                "notification:outbound:EMAIL:" + financialEntityId,
                1000,
                Duration.ofHours(1))).thenReturn(true);
    }

    @Test
    void consumesConfiguredBucketScopedByChannelAndFinancialEntity() {
        limiter.check(NotificationChannel.EMAIL, financialEntityId);

        verify(redis).tryConsumeTokenBucket(
                "notification:outbound:EMAIL:" + financialEntityId,
                1000,
                Duration.ofHours(1));
        verify(securityEvents, never()).record(any(SecurityEvent.class));
    }

    @Test
    void blocksExcessAndRecordsMassSendSecurityEvent() {
        when(redis.tryConsumeTokenBucket(
                "notification:outbound:WHATSAPP:" + financialEntityId,
                500,
                Duration.ofHours(1))).thenReturn(false);

        assertThatThrownBy(() -> limiter.check(NotificationChannel.WHATSAPP, financialEntityId))
                .isInstanceOf(RateLimitExceededException.class);

        var eventCaptor = org.mockito.ArgumentCaptor.forClass(SecurityEvent.class);
        verify(securityEvents).record(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType().name()).isEqualTo("NOTIFICATION_MASS_SEND_DETECTED");
        assertThat(eventCaptor.getValue().getOutcome().name()).isEqualTo("BLOCKED");
    }

    @Test
    void failsClosedWhenRedisIsUnavailable() {
        when(redis.tryConsumeTokenBucket(
                "notification:outbound:EMAIL:" + financialEntityId,
                1000,
                Duration.ofHours(1))).thenThrow(new SecurityInfrastructureException("Redis indisponível"));

        assertThatThrownBy(() -> limiter.check(NotificationChannel.EMAIL, financialEntityId))
                .isInstanceOf(SecurityInfrastructureException.class);
        verify(securityEvents, never()).record(any(SecurityEvent.class));
    }

    @Test
    void rejectsMissingTenantOrChannelBeforeRedisAccess() {
        assertThatThrownBy(() -> limiter.check(NotificationChannel.EMAIL, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limiter.check(null, financialEntityId))
                .isInstanceOf(IllegalArgumentException.class);
        verify(redis, never()).tryConsumeTokenBucket(any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }
}
