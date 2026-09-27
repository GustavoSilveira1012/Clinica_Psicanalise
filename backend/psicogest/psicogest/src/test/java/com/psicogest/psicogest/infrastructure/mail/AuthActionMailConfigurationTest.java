package com.psicogest.psicogest.infrastructure.mail;

import com.psicogest.psicogest.service.AuthActionMailProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthActionMailConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AuthActionMailConfiguration.class);

    @Test
    void registersFailClosedProviderByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AuthActionMailProvider.class);
            assertThat(context.getBean(AuthActionMailProvider.class).isAvailable()).isFalse();
        });
    }

    @Test
    void registersSmtpProviderOnlyWhenEnabledAndConfigurationIsValid() {
        contextRunner
                .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
                .withPropertyValues(
                        "app.auth-actions.mail.enabled=true",
                        "app.auth-actions.mail.from=security@example.invalid",
                        "app.auth-actions.mail.public-base-url=https://pilot.example.invalid",
                        "app.auth-actions.mail.rate-limit-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                        "spring.mail.host=localhost",
                        "spring.mail.username=synthetic-user",
                        "spring.mail.password=synthetic-password",
                        "spring.mail.properties.mail.smtp.auth=true",
                        "spring.mail.properties.mail.smtp.starttls.enable=true",
                        "spring.mail.properties.mail.smtp.starttls.required=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AuthActionMailProvider.class);
                    assertThat(context.getBean(AuthActionMailProvider.class).isAvailable()).isTrue();
                });
    }

    @Test
    void refusesEnabledMailWithoutStrongRateLimitKey() {
        contextRunner
                .withPropertyValues(
                        "app.auth-actions.mail.enabled=true",
                        "app.auth-actions.mail.from=security@example.invalid",
                        "app.auth-actions.mail.public-base-url=https://pilot.example.invalid",
                        "app.auth-actions.mail.rate-limit-key=weak"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void doesNotReportSmtpReadyWithoutTls() {
        contextRunner
                .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
                .withPropertyValues(
                        "app.auth-actions.mail.enabled=true",
                        "app.auth-actions.mail.from=security@example.invalid",
                        "app.auth-actions.mail.public-base-url=https://pilot.example.invalid",
                        "app.auth-actions.mail.rate-limit-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                        "spring.mail.host=localhost",
                        "spring.mail.username=synthetic-user",
                        "spring.mail.password=synthetic-password",
                        "spring.mail.properties.mail.smtp.auth=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AuthActionMailProvider.class).isAvailable()).isFalse();
                });
    }
}
