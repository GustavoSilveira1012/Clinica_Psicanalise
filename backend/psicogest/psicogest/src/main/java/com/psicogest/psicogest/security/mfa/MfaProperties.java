package com.psicogest.psicogest.security.mfa;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.mfa")
public record MfaProperties(Duration challengeTtl, int maxAttempts, String issuer, String encryptionKey) {
    public MfaProperties {
        if (challengeTtl == null || challengeTtl.isNegative() || challengeTtl.isZero()
                || maxAttempts < 1 || issuer == null || issuer.isBlank()
                || encryptionKey == null || encryptionKey.isBlank()) {
            throw new IllegalArgumentException("Configuração MFA incompleta ou inválida");
        }
    }
}
