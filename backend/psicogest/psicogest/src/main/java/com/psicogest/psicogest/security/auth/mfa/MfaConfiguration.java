package com.psicogest.psicogest.security.auth.mfa;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties(MfaProperties.class)
public class MfaConfiguration {
    @Bean public Clock securityClock() { return Clock.systemUTC(); }
}
