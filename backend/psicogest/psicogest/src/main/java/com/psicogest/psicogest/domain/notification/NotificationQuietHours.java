package com.psicogest.psicogest.domain.notification;

import java.time.LocalTime;
import java.time.ZoneId;

public record NotificationQuietHours(
        LocalTime start,
        LocalTime end,
        ZoneId timezone
) {
}
