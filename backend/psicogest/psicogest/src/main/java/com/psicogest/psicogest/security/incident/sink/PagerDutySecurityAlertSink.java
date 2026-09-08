package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityAlertSink;
import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Integração com PagerDuty para escalação automática de incidentes críticos.
 * Requer configuração de API key em environment.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.security.alert.pagerduty.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class PagerDutySecurityAlertSink implements SecurityAlertSink {

    private final PagerDutyService pagerDutyService;

    public PagerDutySecurityAlertSink(
            PagerDutyService pagerDutyService
    ) {
        this.pagerDutyService = pagerDutyService;
    }

    @Override
    public void publish(
            SecurityIncidentNotification notification
    ) {

        // Só escalate para PagerDuty se for crítico
        if ( !notification.isCritical() ) {
            return;
        }

        try {

            pagerDutyService.createIncident(
                    notification
            );

            log.debug(
                    "Incidente escalado para PagerDuty: {}",
                    notification.incidentId()
            );

        } catch ( Exception exception ) {

            log.error(
                    "Falha ao escalhar incidente para PagerDuty",
                    exception
            );
        }
    }

    @Override
    public String name() {
        return "pagerduty";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
