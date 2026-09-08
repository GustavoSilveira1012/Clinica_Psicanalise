package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityAlertSink;
import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingSecurityAlertSink implements SecurityAlertSink {

    @Override
    public void publish(
            SecurityIncidentNotification notification
    ) {

        if ( notification.isCritical() ) {

            log.error(
                    "🚨 CRITICAL SECURITY INCIDENT: {} | Category: {} | "
                    + "ClinicalData: {} | Breach: {} | IncidentId: {}",
                    notification.summary(),
                    notification.category(),
                    notification.involvesClinicalData(),
                    notification.involvesBreach(),
                    notification.incidentId()
            );

        } else if (
                notification.severity()
                        .ordinal() >= 2
        ) {

            log.warn(
                    "⚠️ Security Incident: {} | Severity: {} | IncidentId: {}",
                    notification.summary(),
                    notification.severity(),
                    notification.incidentId()
            );

        } else {

            log.info(
                    "ℹ️ Security Alert: {} | IncidentId: {}",
                    notification.summary(),
                    notification.incidentId()
            );
        }
    }

    @Override
    public String name() {
        return "logging";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
