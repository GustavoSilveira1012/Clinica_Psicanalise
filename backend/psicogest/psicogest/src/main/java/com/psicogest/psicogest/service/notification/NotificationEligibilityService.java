package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.domain.notification.NotificationRecipient;
import com.psicogest.psicogest.model.enums.NotificationChannel;
import com.psicogest.psicogest.model.enums.NotificationType;

public interface NotificationEligibilityService {

    NotificationEligibility evaluate(
            NotificationRecipient recipient,
            NotificationType type,
            NotificationChannel channel
    );
}
