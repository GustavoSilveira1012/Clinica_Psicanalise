package com.psicogest.psicogest.security;

import com.psicogest.psicogest.security.audit.AuditProperties;
import com.psicogest.psicogest.security.auth.mfa.MfaProperties;
import com.psicogest.psicogest.security.config.ProductionKeyMaterialValidator;
import com.psicogest.psicogest.security.crypto.CryptoProperties;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionKeyMaterialValidatorTest {
    private static String key() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static MfaProperties mfa(String key) {
        return new MfaProperties(Duration.ofMinutes(5), 5, "PsicoGest", key);
    }

    @Test
    void acceptsDistinctVersionedKeysWhileRetainingOldOnes() {
        var crypto = new CryptoProperties("secret-managed", "v2", Map.of("primary", key(), "v2", key()));
        var audit = new AuditProperties("v2", Map.of("primary", key(), "v2", key()));
        assertThatCode(() -> ProductionKeyMaterialValidator.validate(crypto, audit, mfa(key())))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsZeroMissingOrReusedMaterial() {
        String zero = Base64.getEncoder().encodeToString(new byte[32]);
        String clinical = key();
        var validClinical = new CryptoProperties("secret-managed", "primary", Map.of("primary", clinical));
        var validAudit = new AuditProperties("primary", Map.of("primary", key()));
        assertThatThrownBy(() -> ProductionKeyMaterialValidator.validate(
                validClinical, validAudit, mfa(zero))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ProductionKeyMaterialValidator.validate(
                new CryptoProperties("secret-managed", "absent", Map.of("primary", clinical)),
                validAudit, mfa(key()))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ProductionKeyMaterialValidator.validate(
                validClinical, new AuditProperties("primary", Map.of("primary", clinical)), mfa(key())))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ProductionKeyMaterialValidator.validate(
                validClinical, validAudit, mfa("not-base64"))).isInstanceOf(IllegalStateException.class);
    }
}
