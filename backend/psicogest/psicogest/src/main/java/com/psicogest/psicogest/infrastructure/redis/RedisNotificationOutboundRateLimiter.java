package com.psicogest.psicogest.infrastructure.redis;

import com.psicogest.psicogest.config.NotificationOutboundRateLimitProperties;
import com.psicogest.psicogest.exception.RateLimitExceededException;
import com.psicogest.psicogest.model.enums.NotificationChannel;
import com.psicogest.psicogest.model.entity.SecurityEvent;
import com.psicogest.psicogest.model.enums.SecurityEventOutcome;
import com.psicogest.psicogest.model.enums.SecurityEventSeverity;
import com.psicogest.psicogest.model.enums.SecurityEventType;
import com.psicogest.psicogest.security.event.SecurityEventService;
import com.psicogest.psicogest.service.notification.NotificationOutboundRateLimiter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Map;

/** Limitação distribuída por entidade financeira e canal; falha fechada. */
@Component
@EnableConfigurationProperties(NotificationOutboundRateLimitProperties.class)
public class RedisNotificationOutboundRateLimiter implements NotificationOutboundRateLimiter {

    private final RedisSecurityRateLimiter rateLimiter;
    private final NotificationOutboundRateLimitProperties properties;
    private final SecurityEventService securityEventService;

    public RedisNotificationOutboundRateLimiter(
            RedisSecurityRateLimiter rateLimiter,
            NotificationOutboundRateLimitProperties properties,
            SecurityEventService securityEventService
    ) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.securityEventService = securityEventService;
    }

    @Override
    public void check(NotificationChannel channel, UUID financialEntityId) {
        if (channel == null || financialEntityId == null) {
            throw new IllegalArgumentException("Canal e entidade financeira são obrigatórios");
        }
        NotificationOutboundRateLimitProperties.Bucket bucket = bucket(channel);
        if (bucket == null || bucket.capacity() <= 0 || bucket.refillPeriod() == null) {
            throw new IllegalStateException("Rate limit outbound não configurado para " + channel);
        }
        String key = "notification:outbound:" + channel.name() + ":" + financialEntityId;
        if (!rateLimiter.tryConsume(key, bucket.capacity(), bucket.refillPeriod())) {
            securityEventService.record(SecurityEvent.builder()
                    .id(UUID.randomUUID())
                    .eventType(SecurityEventType.NOTIFICATION_MASS_SEND_DETECTED)
                    .severity(SecurityEventSeverity.HIGH)
                    .outcome(SecurityEventOutcome.BLOCKED)
                    .occurredAt(LocalDateTime.now())
                    .resourceType("NOTIFICATION_OUTBOUND_RATE_LIMIT")
                    .resourceId(channel.name())
                    .metadata(Map.of(
                            "channel", channel.name(),
                            "financialEntityId", financialEntityId.toString(),
                            "limit", bucket.capacity(),
                            "windowSeconds", bucket.refillPeriod().toSeconds()
                    ))
                    .build());
            throw new RateLimitExceededException("Limite de notificações de saída atingido");
        }
    }

    private NotificationOutboundRateLimitProperties.Bucket bucket(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> properties.email();
            case WHATSAPP -> properties.whatsapp();
            case SMS -> properties.sms();
        };
    }
}
