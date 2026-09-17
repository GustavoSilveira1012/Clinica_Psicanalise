package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.domain.notification.NotificationRecipient;
import com.psicogest.psicogest.domain.notification.NotificationRecipientContext;
import com.psicogest.psicogest.model.enums.NotificationChannel;
import com.psicogest.psicogest.model.enums.NotificationType;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Política fail-closed para comunicações operacionais. Marketing não faz
 * parte do catálogo clínico-operacional e não pode ser inferido por este
 * serviço.
 */
@Service
public class DefaultNotificationEligibilityService implements NotificationEligibilityService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[1-9]\\d{7,14}$");

    @Override
    public NotificationEligibility evaluate(
            NotificationRecipient recipient,
            NotificationType type,
            NotificationChannel channel
    ) {
        if (!(recipient instanceof NotificationRecipientContext context)
                || context.financialEntityId() == null) {
            return suppressed("INVALID_RECIPIENT");
        }
        if (type == null || channel == null) {
            return suppressed("INVALID_NOTIFICATION");
        }
        if (context.destination() == null || context.destination().isBlank()) {
            return suppressed("DESTINATION_MISSING");
        }
        if (!context.required() && !context.preferenceEnabled()) {
            return suppressed("OPT_OUT");
        }
        if (!validDestination(channel, context.destination().trim())) {
            return suppressed("DESTINATION_INVALID");
        }
        return new NotificationEligibility(true, "ELIGIBLE");
    }

    private boolean validDestination(NotificationChannel channel, String destination) {
        return switch (channel) {
            case EMAIL -> EMAIL.matcher(destination.toLowerCase(Locale.ROOT)).matches();
            case WHATSAPP, SMS -> PHONE.matcher(destination.replaceAll("[()\\s-]", "")).matches();
        };
    }

    private NotificationEligibility suppressed(String reason) {
        return new NotificationEligibility(false, reason);
    }
}
