package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.domain.event.DomainEvent;

public interface NotificationEventHandler {

    boolean supports(String eventType);

    void handle(DomainEvent event);
}
