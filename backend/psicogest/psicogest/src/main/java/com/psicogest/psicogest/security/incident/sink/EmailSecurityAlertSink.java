package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityAlertSink;
import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.security.alert.email.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class EmailSecurityAlertSink implements SecurityAlertSink {

    private final EmailNotificationService emailService;

    public EmailSecurityAlertSink(
            EmailNotificationService emailService
    ) {
        this.emailService = emailService;
    }

    @Override
    public void publish(
            SecurityIncidentNotification notification
    ) {

        try {

            emailService.sendSecurityAlert(
                    notification
            );

            log.debug(
                    "Email de alerta de segurança enviado para o incidente {}",
                    notification.incidentId()
            );

        } catch ( Exception exception ) {

            log.error(
                    "Falha ao enviar email de alerta de segurança",
                    exception
            );
        }
    }

    @Override
    public String name() {
        return "email";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
