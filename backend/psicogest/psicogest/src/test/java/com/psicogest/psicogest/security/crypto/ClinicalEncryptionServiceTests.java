package com.psicogest.psicogest.security.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName( "ClinicalEncryptionService" )
public class ClinicalEncryptionServiceTests {

    private ClinicalEncryptionService encryptionService;
    private DataKeyProvider keyProvider;
    private CryptoProperties properties;

    @BeforeEach
    void setup() {
        Map<String, String> keys = new HashMap<>();
        keys.put(
                "clinical-dev-2026",
                "xb0Z/F5bMz0ORlvUJ3F4F3X5Y6Z7A8B9C0D1E2F3G4H5I6J7K8L9M0N1O2P3Q4"
        );
        properties = new CryptoProperties(
                "local",
                "clinical-dev-2026",
                keys
        );
        keyProvider = new LocalDevelopmentDataKeyProvider( properties );
        encryptionService = new ClinicalEncryptionService( keyProvider );
    }

    @Test
    @DisplayName( "plaintext → encrypt → decrypt resultado idêntico" )
    void shouldEncryptAndDecryptCorrectly() {
        String plaintext = "Conteúdo clínico extremamente sensível";
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        EncryptedEnvelope encrypted = encryptionService.encrypt(
                plaintext,
                context
        );
        String decrypted = encryptionService.decrypt( encrypted, context );

        assertThat( decrypted ).isEqualTo( plaintext );
    }

    @Test
    @DisplayName( "mesma mensagem criptografada duas vezes ciphertext diferente" )
    void shouldGenerateDifferentCiphertextsForSamePlaintext() {
        String plaintext = "Conteúdo clínico";
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        EncryptedEnvelope encrypted1 = encryptionService.encrypt(
                plaintext,
                context
        );
        EncryptedEnvelope encrypted2 = encryptionService.encrypt(
                plaintext,
                context
        );

        assertThat( encrypted1.ciphertext() )
                .isNotEqualTo( encrypted2.ciphertext() );
        assertThat( encrypted1.iv() )
                .isNotEqualTo( encrypted2.iv() );
    }

    @Test
    @DisplayName( "IV tem 12 bytes" )
    void shouldHaveIvOfCorrectLength() {
        EncryptedEnvelope encrypted = encryptionService.encrypt(
                "conteúdo",
                new EncryptionContext(
                        "MEDICAL_RECORD",
                        UUID.randomUUID().toString(),
                        100L,
                        "content"
                )
        );

        assertThat( encrypted.iv() ).hasSize( 12 );
    }

    @Test
    @DisplayName( "ciphertext alterado → ClinicalDataIntegrityException" )
    void shouldDetectTamperedCiphertext() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );
        EncryptedEnvelope encrypted = encryptionService.encrypt(
                "conteúdo",
                context
        );

        byte[] tamperedCiphertext = encrypted.ciphertext();
        tamperedCiphertext[0] ^= 1;

        EncryptedEnvelope tampered = new EncryptedEnvelope(
                encrypted.cryptoVersion(),
                encrypted.algorithm(),
                encrypted.keyId(),
                encrypted.wrappedDataKey(),
                encrypted.iv(),
                tamperedCiphertext
        );

        assertThatThrownBy(
                () -> encryptionService.decrypt( tampered, context )
        )
                .isInstanceOf( ClinicalDataIntegrityException.class );
    }

    @Test
    @DisplayName( "IV alterado → ClinicalDataIntegrityException" )
    void shouldDetectTamperedIv() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );
        EncryptedEnvelope encrypted = encryptionService.encrypt(
                "conteúdo",
                context
        );

        byte[] tamperedIv = encrypted.iv();
        tamperedIv[0] ^= 1;

        EncryptedEnvelope tampered = new EncryptedEnvelope(
                encrypted.cryptoVersion(),
                encrypted.algorithm(),
                encrypted.keyId(),
                encrypted.wrappedDataKey(),
                tamperedIv,
                encrypted.ciphertext()
        );

        assertThatThrownBy(
                () -> encryptionService.decrypt( tampered, context )
        )
                .isInstanceOf( ClinicalDataIntegrityException.class );
    }

    @Test
    @DisplayName( "AAD patientId alterada → ClinicalDataIntegrityException" )
    void shouldPreventPatientIdMismatch() {
        String plaintext = "Conteúdo clínico";
        EncryptionContext contextA = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        EncryptedEnvelope encrypted = encryptionService.encrypt(
                plaintext,
                contextA
        );

        EncryptionContext contextB = new EncryptionContext(
                "MEDICAL_RECORD",
                contextA.resourceId(),
                200L,
                "content"
        );

        assertThatThrownBy(
                () -> encryptionService.decrypt( encrypted, contextB )
        )
                .isInstanceOf( ClinicalDataIntegrityException.class );
    }

    @Test
    @DisplayName( "resourceId alterado → ClinicalDataIntegrityException" )
    void shouldPreventResourceIdMismatch() {
        String plaintext = "Conteúdo clínico";
        String resourceIdA = UUID.randomUUID().toString();
        String resourceIdB = UUID.randomUUID().toString();

        EncryptionContext contextA = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceIdA,
                100L,
                "content"
        );

        EncryptedEnvelope encrypted = encryptionService.encrypt(
                plaintext,
                contextA
        );

        EncryptionContext contextB = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceIdB,
                100L,
                "content"
        );

        assertThatThrownBy(
                () -> encryptionService.decrypt( encrypted, contextB )
        )
                .isInstanceOf( ClinicalDataIntegrityException.class );
    }

    @Test
    @DisplayName( "encrypted DEK trocada entre records → falha" )
    void shouldPreventCiphertextSwapBetweenRecords() {
        EncryptionContext contextA = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );
        EncryptionContext contextB = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                200L,
                "content"
        );

        EncryptedEnvelope encryptedA = encryptionService.encrypt(
                "Conteúdo A",
                contextA
        );
        EncryptedEnvelope encryptedB = encryptionService.encrypt(
                "Conteúdo B",
                contextB
        );

        // Tenta usar DEK de B com contexto de A
        EncryptedEnvelope swapped = new EncryptedEnvelope(
                encryptedA.cryptoVersion(),
                encryptedA.algorithm(),
                encryptedB.keyId(),
                encryptedB.wrappedDataKey(),
                encryptedA.iv(),
                encryptedA.ciphertext()
        );

        assertThatThrownBy(
                () -> encryptionService.decrypt( swapped, contextA )
        )
                .isInstanceOf( ClinicalDataIntegrityException.class );
    }

    @Test
    @DisplayName( "cryptoVersion inválida → rejeitada" )
    void shouldRejectInvalidCryptoVersion() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );
        EncryptedEnvelope encrypted = encryptionService.encrypt(
                "conteúdo",
                context
        );

        EncryptedEnvelope invalidVersion = new EncryptedEnvelope(
                99,
                encrypted.algorithm(),
                encrypted.keyId(),
                encrypted.wrappedDataKey(),
                encrypted.iv(),
                encrypted.ciphertext()
        );

        assertThatThrownBy(
                () -> encryptionService.decrypt( invalidVersion, context )
        )
                .isInstanceOf( ClinicalEncryptionException.class )
                .hasMessageContaining( "Versão criptográfica não suportada" );
    }

    @Test
    @DisplayName( "algorithm inválido → rejeitado" )
    void shouldRejectInvalidAlgorithm() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );
        EncryptedEnvelope encrypted = encryptionService.encrypt(
                "conteúdo",
                context
        );

        EncryptedEnvelope invalidAlgorithm = new EncryptedEnvelope(
                encrypted.cryptoVersion(),
                "AES-256-CBC",
                encrypted.keyId(),
                encrypted.wrappedDataKey(),
                encrypted.iv(),
                encrypted.ciphertext()
        );

        assertThatThrownBy(
                () -> encryptionService.decrypt( invalidAlgorithm, context )
        )
                .isInstanceOf( ClinicalEncryptionException.class )
                .hasMessageContaining( "Algoritmo criptográfico não suportado" );
    }

    @Test
    @DisplayName( "plaintext null → IllegalArgumentException" )
    void shouldRejectNullPlaintext() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        assertThatThrownBy(
                () -> encryptionService.encrypt( null, context )
        )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    @DisplayName( "fieldName alterado → ClinicalDataIntegrityException" )
    void shouldPreventFieldNameMismatch() {
        String plaintext = "Conteúdo clínico";
        String resourceId = UUID.randomUUID().toString();

        EncryptionContext contextA = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                100L,
                "content"
        );

        EncryptedEnvelope encrypted = encryptionService.encrypt(
                plaintext,
                contextA
        );

        EncryptionContext contextB = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                100L,
                "addendum"
        );

        assertThatThrownBy(
                () -> encryptionService.decrypt( encrypted, contextB )
        )
                .isInstanceOf( ClinicalDataIntegrityException.class );
    }
}
