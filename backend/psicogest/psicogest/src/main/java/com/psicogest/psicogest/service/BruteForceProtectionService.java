package com.psicogest.psicogest.service;

import com.psicogest.psicogest.infrastructure.security.SecurityHashService;
import com.psicogest.psicogest.model.entity.SecurityEvent;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.SecurityEventOutcome;
import com.psicogest.psicogest.model.enums.SecurityEventSeverity;
import com.psicogest.psicogest.model.enums.SecurityEventType;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.security.event.SecurityEventService;
import com.psicogest.psicogest.security.request.SecurityRequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class BruteForceProtectionService {

    private final UserRepository userRepository;
    private final SecurityEventService eventService;
    private final SecurityHashService hashService;

    public BruteForceProtectionService(
            UserRepository userRepository,
            SecurityEventService eventService,
            SecurityHashService hashService
    ) {
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.hashService = hashService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailure(
            User knownUser,
            String normalizedEmail,
            SecurityRequestContext context
    ) {
        int attempts = 1;
        User user = null;

        if (knownUser != null) {
            user = userRepository
                    .findByIdForSecurityUpdate(knownUser.getId())
                    .orElseThrow();

            user.setFailedLoginAttempts(
                    user.getFailedLoginAttempts() + 1
            );
            user.setLastFailedLoginAt(LocalDateTime.now());
            attempts = user.getFailedLoginAttempts();

            userRepository.save(user);
        }

        SecurityEventSeverity severity;
        if (attempts >= 10) {
            severity = SecurityEventSeverity.HIGH;
        } else if (attempts >= 5) {
            severity = SecurityEventSeverity.MEDIUM;
        } else {
            severity = SecurityEventSeverity.LOW;
        }

        eventService.record(
                SecurityEvent.builder()
                        .id(UUID.randomUUID())
                        .user(user)
                        .eventType(SecurityEventType.LOGIN_FAILURE)
                        .severity(severity)
                        .outcome(SecurityEventOutcome.FAILURE)
                        .occurredAt(LocalDateTime.now())
                        .sourceIp(context.sourceIp())
                        .userAgentHash(context.userAgentHash())
                        .requestMethod(context.method())
                        .requestPath(context.path())
                        .metadata(Map.of(
                                "accountIdentifierHash", hashService.sha256(normalizedEmail),
                                "consecutiveFailures", attempts
                        ))
                        .build()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerSuccess(
            User userReference,
            SecurityRequestContext context
    ) {
        User user = userRepository
                .findByIdForSecurityUpdate(userReference.getId())
                .orElseThrow();

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user);

        eventService.record(
                SecurityEvent.builder()
                        .id(UUID.randomUUID())
                        .user(user)
                        .eventType(SecurityEventType.LOGIN_SUCCESS)
                        .severity(SecurityEventSeverity.INFO)
                        .outcome(SecurityEventOutcome.SUCCESS)
                        .occurredAt(LocalDateTime.now())
                        .sourceIp(context.sourceIp())
                        .userAgentHash(context.userAgentHash())
                        .requestMethod(context.method())
                        .requestPath(context.path())
                        .build()
        );
    }
}
