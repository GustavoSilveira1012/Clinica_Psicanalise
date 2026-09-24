package com.psicogest.psicogest.security.crypto;

import org.junit.jupiter.api.Test;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class SecretManagedDataKeyProviderTest {
    private String key() {
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
    private EncryptionContext context(String org) {
        return new EncryptionContext("MEDICAL_RECORD", "123", "content", Map.of("organizationId", org));
    }
    private ApplicationEncryptionService encryption(String current, Map<String, String> keys) {
        return new ApplicationEncryptionService(new SecretManagedDataKeyProvider(
                new CryptoProperties("secret-managed", current, keys)));
    }
    @Test void roundTripPreservesContent() {
        var service = encryption("primary", Map.of("primary", key()));
        var envelope = service.encrypt("synthetic clinical test", context("a"));
        assertThat(service.decrypt(envelope, context("a"))).isEqualTo("synthetic clinical test");
    }
    @Test void differentTenantCannotDecryptEnvelope() {
        var service = encryption("primary", Map.of("primary", key()));
        var envelope = service.encrypt("synthetic clinical test", context("a"));
        assertThatThrownBy(() -> service.decrypt(envelope, context("b")))
                .isInstanceOf(ClinicalEncryptionException.class);
    }
    @Test void retainedOldKeyCanDecryptAfterRotation() {
        String old = key(), next = key();
        var envelope = encryption("old", Map.of("old", old)).encrypt("synthetic test", context("a"));
        var rotated = encryption("next", Map.of("old", old, "next", next));
        assertThat(rotated.decrypt(envelope, context("a"))).isEqualTo("synthetic test");
        assertThat(rotated.encrypt("new test", context("a")).keyId()).isEqualTo("next");
    }
    @Test void rejectsMissingCurrentKey() {
        assertThatThrownBy(() -> encryption("absent", Map.of("primary", key())))
                .isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectsZeroOrInvalidKeysAtStartup() {
        for (String invalid : new String[]{Base64.getEncoder().encodeToString(new byte[32]), "not-base64!", "YWJj"}) {
            assertThatThrownBy(() -> encryption("primary", Map.of("primary", invalid)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
