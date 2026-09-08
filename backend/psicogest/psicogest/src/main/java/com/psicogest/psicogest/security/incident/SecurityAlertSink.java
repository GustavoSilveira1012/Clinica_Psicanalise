package com.psicogest.psicogest.security.incident;

public interface SecurityAlertSink {

    void publish(
            SecurityIncidentNotification notification
    );

    /**
     * Nome único do sink para logging e identificação
     */
    String name();

    /**
     * Se este sink está habilitado
     */
    boolean isEnabled();
}
