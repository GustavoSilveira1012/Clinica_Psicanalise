package com.psicogest.psicogest.service.notification;

import com.psicogest.psicogest.domain.notification.NotificationRecipientContext;
import com.psicogest.psicogest.domain.notification.NotificationTemplate;
import com.psicogest.psicogest.domain.notification.RenderedNotification;
import com.psicogest.psicogest.model.enums.NotificationChannel;
import com.psicogest.psicogest.model.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationPolicyTest {

    private final SafeNotificationTemplateRenderer renderer =
            new SafeNotificationTemplateRenderer();
    private final DefaultNotificationEligibilityService eligibility =
            new DefaultNotificationEligibilityService();

    @Test
    void rendersOnlyAllowlistedPlaceholders() {
        RenderedNotification rendered = renderer.render(
                new NotificationTemplate(
                        "Consulta em {{appointmentDate}}",
                        "Olá {{patientFirstName}}, sua consulta é às {{appointmentTime}}."),
                Map.of(
                        "patientFirstName", "Marina",
                        "appointmentDate", "18/09/2026",
                        "appointmentTime", "09:00"));

        assertThat(rendered.subject()).isEqualTo("Consulta em 18/09/2026");
        assertThat(rendered.body()).isEqualTo("Olá Marina, sua consulta é às 09:00.");
    }

    @Test
    void rejectsExpressionsAndUnknownPlaceholders() {
        assertThatThrownBy(() -> renderer.render(
                new NotificationTemplate("{{unknown}}", "texto"),
                Map.of("unknown", "valor")))
                .isInstanceOf(NotificationTemplateException.class);

        assertThatThrownBy(() -> renderer.render(
                new NotificationTemplate("${7 * 7}", "texto"),
                Map.of()))
                .isInstanceOf(NotificationTemplateException.class);
    }

    @Test
    void suppressesOptionalOptedOutMessagesButAllowsRequiredMessages() {
        UUID financialEntityId = UUID.randomUUID();
        NotificationRecipientContext optedOut = new NotificationRecipientContext(
                financialEntityId, "marina@example.com", false, false);
        NotificationRecipientContext required = new NotificationRecipientContext(
                financialEntityId, "marina@example.com", false, true);

        assertThat(eligibility.evaluate(
                optedOut, NotificationType.PAYMENT_REMINDER, NotificationChannel.EMAIL))
                .satisfies(result -> {
                    assertThat(result.allowed()).isFalse();
                    assertThat(result.reasonCode()).isEqualTo("OPT_OUT");
                });
        assertThat(eligibility.evaluate(
                required, NotificationType.PAYMENT_REMINDER, NotificationChannel.EMAIL).allowed())
                .isTrue();
    }

    @Test
    void handlesQuietHoursThatCrossMidnight() {
        var quietHours = new com.psicogest.psicogest.domain.notification.NotificationQuietHours(
                LocalTime.of(22, 0), LocalTime.of(7, 0), ZoneId.of("America/Sao_Paulo"));

        assertThat(NotificationQuietHoursPolicy.isQuiet(
                quietHours, Instant.parse("2026-09-18T02:00:00Z"))).isTrue();
        assertThat(NotificationQuietHoursPolicy.isQuiet(
                quietHours, Instant.parse("2026-09-18T15:00:00Z"))).isFalse();
    }
}
