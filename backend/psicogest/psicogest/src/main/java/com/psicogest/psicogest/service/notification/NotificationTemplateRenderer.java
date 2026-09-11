package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.domain.notification.NotificationTemplate;
import com.psicogest.psicogest.domain.notification.RenderedNotification;

import java.util.Map;

public interface NotificationTemplateRenderer {

    RenderedNotification render(
            NotificationTemplate template,
            Map<String, String> variables
    );
}
