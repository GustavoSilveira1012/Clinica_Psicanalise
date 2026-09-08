package com.psicogest.psicogest.security.incident.sink;

import com.psicogest.psicogest.security.incident.SecurityAlertSink;
import com.psicogest.psicogest.security.incident.SecurityIncidentNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Integração com cloud monitoring (AWS CloudWatch, Google Cloud Monitoring, Azure Monitor).
 * Permite correlação com métricas infraestruturais.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.security.alert.cloud-monitoring.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class CloudMonitoringAlertSink implements SecurityAlertSink {

    private final CloudMonitoringService cloudMonitoringService;

    public CloudMonitoringAlertSink(
            CloudMonitoringService cloudMonitoringService
    ) {
        this.cloudMonitoringService = cloudMonitoringService;
    }

    @Override
    public void publish(
            SecurityIncidentNotification notification
    ) {

        try {

            cloudMonitoringService.publishMetric(
                    notification
            );

            log.debug(
                    "Métrica de incidente publicada para cloud monitoring: {}",
                    notification.incidentId()
            );

        } catch ( Exception exception ) {

            log.error(
                    "Falha ao publicar métrica para cloud monitoring",
                    exception
            );
        }
    }

    @Override
    public String name() {
        return "cloud-monitoring";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
