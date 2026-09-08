package com.psicogest.psicogest.security.incident;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SecurityAlertPublisher {

    private final List<SecurityAlertSink> sinks;

    public SecurityAlertPublisher(
            List<SecurityAlertSink> sinks
    ) {
        this.sinks = sinks;
    }

    public void publish(
            SecurityIncidentNotification notification
    ) {

        for ( SecurityAlertSink sink : sinks ) {

            if ( !sink.isEnabled() ) {
                continue;
            }

            try {

                sink.publish( notification );

            } catch ( Exception exception ) {

                log.error(
                        "Erro ao publicar alerta no sink '{}' para o incidente {}",
                        sink.name(),
                        notification.incidentId(),
                        exception
                );
            }
        }
    }

    public void publishIfCritical(
            SecurityIncidentNotification notification
    ) {

        if ( notification.isCritical() ) {
            publish( notification );
        }
    }
}
