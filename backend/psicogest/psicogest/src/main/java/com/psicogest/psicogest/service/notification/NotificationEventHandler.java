package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.domain.event.DomainEvent;

/**
 * Handles only the notification side effect of a stored domain event. Handlers
 * run at least once and therefore must be idempotent (normally using the
 * notification deduplication key); provider/network delivery belongs to a
 * separate delivery worker.
 */
public interface NotificationEventHandler {

    boolean supports(String eventType);

    void handle(DomainEvent event);
}
