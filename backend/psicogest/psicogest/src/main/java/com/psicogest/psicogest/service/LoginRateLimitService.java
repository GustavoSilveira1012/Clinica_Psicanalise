package com.psicogest.psicogest.service;

import com.psicogest.psicogest.config.RateLimitProperties;
import com.psicogest.psicogest.exception.RateLimitExceededException;
import com.psicogest.psicogest.infrastructure.redis.RedisSecurityRateLimiter;
import com.psicogest.psicogest.infrastructure.security.SecurityHashService;
import com.psicogest.psicogest.model.entity.SecurityEvent;
import com.psicogest.psicogest.model.enums.SecurityEventOutcome;
import com.psicogest.psicogest.model.enums.SecurityEventSeverity;
import com.psicogest.psicogest.model.enums.SecurityEventType;
import com.psicogest.psicogest.security.event.SecurityEventService;
import com.psicogest.psicogest.security.request.SecurityRequestContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class LoginRateLimitService {

    private final RedisSecurityRateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final SecurityHashService hashService;
    private final SecurityEventService eventService;

    public LoginRateLimitService(
            RedisSecurityRateLimiter rateLimiter,
            RateLimitProperties properties,
            SecurityHashService hashService,
            SecurityEventService eventService
    ) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.hashService = hashService;
        this.eventService = eventService;
    }

    public void assertAllowed(
            String normalizedEmail,
            SecurityRequestContext context
    ) {
        String ipHash = hashService.sha256(context.sourceIp());
        String accountHash = hashService.sha256(normalizedEmail);

        RateLimitProperties.Bucket ipPolicy = properties.getLoginIp();
        RateLimitProperties.Bucket accountPolicy = properties.getLoginAccount();

        boolean ipAllowed = rateLimiter.tryConsume(
                "security:login:ip:" + ipHash,
                ipPolicy.getCapacity(),
                ipPolicy.getRefillPeriod()
        );

        boolean accountAllowed = rateLimiter.tryConsume(
                "security:login:account:" + accountHash,
                accountPolicy.getCapacity(),
                accountPolicy.getRefillPeriod()
        );

        if (!ipAllowed || !accountAllowed) {
            eventService.record(
                    SecurityEvent.builder()
                            .id(UUID.randomUUID())
                            .eventType(SecurityEventType.RATE_LIMIT_TRIGGERED)
                            .severity(SecurityEventSeverity.HIGH)
                            .outcome(SecurityEventOutcome.BLOCKED)
                            .occurredAt(LocalDateTime.now())
                            .sourceIp(context.sourceIp())
                            .userAgentHash(context.userAgentHash())
                            .requestMethod(context.method())
                            .requestPath(context.path())
                            .metadata(Map.of(
                                    "accountIdentifierHash", accountHash,
                                    "ipBucketExceeded", !ipAllowed,
                                    "accountBucketExceeded", !accountAllowed
                            ))
                            .build()
            );

            throw new RateLimitExceededException();
        }
    }
}
