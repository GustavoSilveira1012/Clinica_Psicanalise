package com.psicogest.psicogest.service.notification;

public record NotificationEligibility(
        boolean allowed,
        String reasonCode
) {
}
