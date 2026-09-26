package com.psicogest.psicogest.service.notification;

/** Operational appointment events that may drive notification workflows. */
public enum AppointmentDomainEventType {
    APPOINTMENT_CREATED,
    APPOINTMENT_CONFIRMED,
    APPOINTMENT_CANCELLED,
    APPOINTMENT_RESCHEDULED
}
