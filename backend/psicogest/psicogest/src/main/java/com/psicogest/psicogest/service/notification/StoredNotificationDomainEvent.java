package com.psicogest.psicogest.service.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psicogest.psicogest.domain.event.DomainEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Immutable event snapshot read from the tenant-scoped domain outbox. */
public record StoredNotificationDomainEvent(
        UUID id,
        String aggregateId,
        String eventType,
        Instant occurredAt,
        Map<String, Object> payload
) implements DomainEvent {

    static StoredNotificationDomainEvent from(
            UUID id,
            String aggregateId,
            String eventType,
            Instant occurredAt,
            String payloadJson,
            ObjectMapper objectMapper
    ) {
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    payloadJson,
                    new TypeReference<>() { }
            );
            return new StoredNotificationDomainEvent(
                    id, aggregateId, eventType, occurredAt,
                    Collections.unmodifiableMap(new LinkedHashMap<>(payload)));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Payload do evento de domínio inválido", exception);
        }
    }
}
