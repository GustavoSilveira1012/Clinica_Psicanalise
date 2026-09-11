package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.domain.notification.NotificationTemplate;
import com.psicogest.psicogest.domain.notification.RenderedNotification;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class SafeNotificationTemplateRenderer
        implements NotificationTemplateRenderer {

    private static final Set<String> ALLOWED_PLACEHOLDERS = Set.of(
            "patientFirstName",
            "appointmentDate",
            "appointmentTime",
            "amount",
            "dueDate"
    );

    @Override
    public RenderedNotification render(
            NotificationTemplate template,
            Map<String, String> variables
    ) {
        Objects.requireNonNull(template, "template não pode ser nulo");
        Objects.requireNonNull(variables, "variables não pode ser nulo");

        return new RenderedNotification(
                renderField(
                        template.subjectTemplate(),
                        variables,
                        "subjectTemplate"
                ),
                renderField(
                        template.bodyTemplate(),
                        variables,
                        "bodyTemplate"
                )
        );
    }

    private String renderField(
            String template,
            Map<String, String> variables,
            String fieldName
    ) {
        if (template == null) {
            return null;
        }

        StringBuilder rendered = new StringBuilder(template.length());
        int index = 0;

        while (index < template.length()) {
            char current = template.charAt(index);

            if (current == '{') {
                if (
                        index + 1 >= template.length()
                                || template.charAt(index + 1) != '{'
                ) {
                    throw invalidTemplate(
                            fieldName,
                            "delimitador de abertura inválido"
                    );
                }

                int closingDelimiter = template.indexOf("}}", index + 2);

                if (closingDelimiter < 0) {
                    throw invalidTemplate(
                            fieldName,
                            "placeholder não finalizado"
                    );
                }

                String placeholder = template.substring(
                        index + 2,
                        closingDelimiter
                );

                if (!ALLOWED_PLACEHOLDERS.contains(placeholder)) {
                    throw invalidTemplate(
                            fieldName,
                            "placeholder não permitido: " + placeholder
                    );
                }

                String value = variables.get(placeholder);

                if (value == null) {
                    throw invalidTemplate(
                            fieldName,
                            "variável ausente: " + placeholder
                    );
                }

                rendered.append(value);
                index = closingDelimiter + 2;
                continue;
            }

            if (current == '}') {
                throw invalidTemplate(
                        fieldName,
                        "delimitador de fechamento inválido"
                );
            }

            rendered.append(current);
            index++;
        }

        return rendered.toString();
    }

    private NotificationTemplateException invalidTemplate(
            String fieldName,
            String reason
    ) {
        return new NotificationTemplateException(
                "Template de notificação inválido em "
                        + fieldName
                        + ": "
                        + reason
        );
    }
}
