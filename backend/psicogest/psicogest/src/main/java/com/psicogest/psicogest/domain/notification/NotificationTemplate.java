package com.psicogest.psicogest.domain.notification;

/**
 * Conteúdo de um template de notificação.
 *
 * A renderização dos campos é responsabilidade de
 * {@code NotificationTemplateRenderer}.
 */
public record NotificationTemplate(
        String subjectTemplate,
        String bodyTemplate
) {
}
