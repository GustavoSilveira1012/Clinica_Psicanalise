package com.psicogest.psicogest.infrastructure.mail;

import com.psicogest.psicogest.config.AuthActionMailProperties;
import com.psicogest.psicogest.service.AuthActionMailProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@EnableConfigurationProperties(AuthActionMailProperties.class)
public class AuthActionMailConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.auth-actions.mail", name = "enabled", havingValue = "true")
    AuthActionMailProvider smtpAuthActionMailProvider(
            JavaMailSender mailSender,
            AuthActionMailProperties properties,
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean authenticationRequired,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean startTlsEnabled,
            @Value("${spring.mail.properties.mail.smtp.starttls.required:false}") boolean startTlsRequired,
            @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}") boolean sslEnabled
    ) {
        return new SmtpAuthActionMailProvider(mailSender, properties.from(), host, port, username, password,
                authenticationRequired, startTlsEnabled, startTlsRequired, sslEnabled);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.auth-actions.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
    AuthActionMailProvider disabledAuthActionMailProvider() {
        return new DisabledAuthActionMailProvider();
    }
}
