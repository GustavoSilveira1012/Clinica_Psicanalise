package com.psicogest.psicogest.infrastructure.notification.provider;

import com.psicogest.psicogest.model.enums.NotificationChannel;

public interface NotificationProvider {

    NotificationChannel channel();

    NotificationSendResult send(NotificationSendCommand command);
}
