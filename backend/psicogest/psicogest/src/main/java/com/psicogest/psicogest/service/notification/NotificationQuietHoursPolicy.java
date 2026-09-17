package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.domain.notification.NotificationQuietHours;

import java.time.Instant;

/** Avalia horários de silêncio inclusive quando o intervalo atravessa meia-noite. */
public final class NotificationQuietHoursPolicy {

    private NotificationQuietHoursPolicy() {
    }

    public static boolean isQuiet(NotificationQuietHours quietHours, Instant instant) {
        if (quietHours == null || instant == null || quietHours.start() == null
                || quietHours.end() == null || quietHours.timezone() == null) {
            return false;
        }
        var time = instant.atZone(quietHours.timezone()).toLocalTime();
        if (quietHours.start().equals(quietHours.end())) return true;
        if (quietHours.start().isBefore(quietHours.end())) {
            return !time.isBefore(quietHours.start()) && time.isBefore(quietHours.end());
        }
        return !time.isBefore(quietHours.start()) || time.isBefore(quietHours.end());
    }
}
