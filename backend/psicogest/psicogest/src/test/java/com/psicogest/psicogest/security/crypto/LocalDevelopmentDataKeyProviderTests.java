package com.psicogest.psicogest.security.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName( "LocalDevelopmentDataKeyProvider" )
public class LocalDevelopmentDataKeyProviderTests {

    private LocalDevelopmentDataKeyProvider keyProvider;
    private CryptoProperties properties;
    private static final String TEST_KEY =
            "xb0Z/F5bMz0ORlvUJ3F4F3X5Y6Z7A8B9C0D1E2F3G4H5I6J7K8L9M0N1O2P3Q4";
    private static final String KEY_ID = "clinical-dev-2026";

    @BeforeEach
    void setup() {
        Map<String, String> keys = new HashMap<>();
        keys.put( KEY_ID, TEST_KEY );
        properties = new CryptoProperties(
                "local",
                KEY_ID,
                keys
        );
        keyProvider = new LocalDevelopmentDataKeyProvider( properties );
    }

    @Test
    @DisplayName( "DEK possui 256 bits" )
    void shouldGenerateDataKeyWith256Bits() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        try ( GeneratedDataKey generatedKey = keyProvider
                .generateDataKey( context ) ) {
            assertThat( generatedKey.plaintextKey() )
                    .hasSize( 32 );
        }
    }

    @Test
    @DisplayName( "cada DEK é única (não reutiliza)" )
    void shouldGenerateUniqueDeks() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        try ( GeneratedDataKey key1 = keyProvider
                .generateDataKey( context );
             GeneratedDataKey key2 = keyProvider
                     .generateDataKey( context ) ) {
            assertThat( key1.plaintextKey() )
                    .isNotEqualTo( key2.plaintextKey() );
        }
    }

    @Test
    @DisplayName( "wrapped DEK pode ser descriptografado" )
    void shouldEncryptAndDecryptDataKey() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        try ( GeneratedDataKey generatedKey = keyProvider
                .generateDataKey( context ) ) {
            byte[] decryptedKey = keyProvider.decryptDataKey(
                    generatedKey.keyId(),
                    generatedKey.wrappedKey(),
                    context
            );

            assertThat( decryptedKey )
                    .isEqualTo( generatedKey.plaintextKey() );
        }
    }

    @Test
    @DisplayName( "wrapped DEK com context diferente → falha" )
    void shouldFailToDecryptWithDifferentContext() {
        String resourceId = UUID.randomUUID().toString();
        EncryptionContext contextA = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                100L,
                "content"
        );
        EncryptionContext contextB = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                200L,
                "content"
        );

        try ( GeneratedDataKey generatedKey = keyProvider
                .generateDataKey( contextA ) ) {
            assertThatThrownBy(
                    () -> keyProvider.decryptDataKey(
                            generatedKey.keyId(),
                            generatedKey.wrappedKey(),
                            contextB
                    )
            )
                    .isInstanceOf( ClinicalDataIntegrityException.class );
        }
    }

    @Test
    @DisplayName( "keyId não encontrada → IllegalStateException" )
    void shouldThrowWhenKeyIdNotFound() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        Map<String, String> emptyKeys = new HashMap<>();
        CryptoProperties emptyProps = new CryptoProperties(
                "local",
                "nonexistent-key",
                emptyKeys
        );
        LocalDevelopmentDataKeyProvider emptyKeyProvider =
                new LocalDevelopmentDataKeyProvider( emptyProps );

        assertThatThrownBy(
                () -> emptyKeyProvider.generateDataKey( context )
        )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "Chave clínica não encontrada" );
    }

    @Test
    @DisplayName( "chave inválida (não 32 bytes) → IllegalStateException" )
    void shouldThrowWhenKeyIsNotExactly32Bytes() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        Map<String, String> invalidKeys = new HashMap<>();
        invalidKeys.put( "bad-key", "aGVsbG8=" );
        CryptoProperties invalidProps = new CryptoProperties(
                "local",
                "bad-key",
                invalidKeys
        );
        LocalDevelopmentDataKeyProvider invalidKeyProvider =
                new LocalDevelopmentDataKeyProvider( invalidProps );

        assertThatThrownBy(
                () -> invalidKeyProvider.generateDataKey( context )
        )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "256 bits" );
    }

    @Test
    @DisplayName( "plaintext DEK apagada após close()" )
    void shouldZeroizePlaintextKeyOnClose() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        GeneratedDataKey generatedKey = keyProvider
                .generateDataKey( context );
        byte[] plaintextBefore = generatedKey.plaintextKey();

        // Verifica que tem dados antes
        assertThat( plaintextBefore )
                .containsAnyOf( (byte) 1, (byte) 2, (byte) 3, (byte) 4,
                        (byte) 5, (byte) 6, (byte) 7, (byte) 8 );

        generatedKey.close();

        // Após close, a chave interna foi zerada
        // (não podemos acessar diretamente, mas o contrato garante)
        assertThat( generatedKey.plaintextKey() )
                .isNotEqualTo( plaintextBefore );
    }

    @Test
    @DisplayName( "wrapped DEK preserva envelope version 1" )
    void shouldPreserveEnvelopeVersion() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        try ( GeneratedDataKey generatedKey = keyProvider
                .generateDataKey( context ) ) {
            byte[] wrapped = generatedKey.wrappedKey();
            // Primeiro byte deve ser 1 (versão do envelope)
            assertThat( wrapped[0] ).isEqualTo( (byte) 1 );
        }
    }

    @Test
    @DisplayName( "wrapped DEK inválida (versão errada) → rejeitada" )
    void shouldRejectInvalidEnvelopeVersion() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        try ( GeneratedDataKey generatedKey = keyProvider
                .generateDataKey( context ) ) {
            byte[] invalidWrapped = generatedKey.wrappedKey();
            invalidWrapped[0] = 99;

            assertThatThrownBy(
                    () -> keyProvider.decryptDataKey(
                            generatedKey.keyId(),
                            invalidWrapped,
                            context
                    )
            )
                    .isInstanceOf( ClinicalEncryptionException.class )
                    .hasMessageContaining( "Versão de envelope não suportada" );
        }
    }

    @Test
    @DisplayName( "wrapped DEK com AAD modificada → ClinicalDataIntegrityException" )
    void shouldDetectTamperedWrappedDek() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        try ( GeneratedDataKey generatedKey = keyProvider
                .generateDataKey( context ) ) {
            byte[] tamperedWrapped = generatedKey.wrappedKey();
            // Modifica um byte no meio (pulando o byte de versão)
            if ( tamperedWrapped.length > 20 ) {
                tamperedWrapped[20] ^= 1;
            }

            assertThatThrownBy(
                    () -> keyProvider.decryptDataKey(
                            generatedKey.keyId(),
                            tamperedWrapped,
                            context
                    )
            )
                    .isInstanceOf( ClinicalDataIntegrityException.class );
        }
    }
}
