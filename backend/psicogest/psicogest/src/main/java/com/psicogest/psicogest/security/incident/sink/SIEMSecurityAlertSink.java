package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityAlertSink;
import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Integração com SIEM (Security Information and Event Management).
 * Exemplos: Splunk, Datadog, Elastic Stack, etc.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.security.alert.siem.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class SIEMSecurityAlertSink implements SecurityAlertSink {

    private final SIEMForwardingService siemService;

    public SIEMSecurityAlertSink(
            SIEMForwardingService siemService
    ) {
        this.siemService = siemService;
    }

    @Override
    public void publish(
            SecurityIncidentNotification notification
    ) {

        try {

            siemService.forwardIncident(
                    notification
            );

            log.debug(
                    "Incidente enviado para SIEM: {}",
                    notification.incidentId()
            );

        } catch ( Exception exception ) {

            log.error(
                    "Falha ao enviar incidente para SIEM",
                    exception
            );
        }
    }

    @Override
    public String name() {
        return "siem";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
