package com.psicogest.psicogest.security.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.security.SecureRandom;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

@DisplayName( "EncryptedEnvelope" )
public class EncryptedEnvelopeTests {

    private static final SecureRandom RANDOM = new SecureRandom();

    private byte[] generateRandomBytes( int length ) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes( bytes );
        return bytes;
    }

    @Test
    @DisplayName( "valores são defensivamente copiados na construção" )
    void shouldDefensivelyCloneArraysOnConstruction() {
        byte[] wrappedDataKey = generateRandomBytes( 64 );
        byte[] iv = generateRandomBytes( 12 );
        byte[] ciphertext = generateRandomBytes( 100 );

        byte[] wrappedDataKeyCopy = wrappedDataKey.clone();
        byte[] ivCopy = iv.clone();
        byte[] ciphertextCopy = ciphertext.clone();

        EncryptedEnvelope envelope = new EncryptedEnvelope(
                1,
                "AES-256-GCM",
                "key-2026",
                wrappedDataKey,
                iv,
                ciphertext
        );

        // Modifica as arrays originais
        Arrays.fill( wrappedDataKey, (byte) 0 );
        Arrays.fill( iv, (byte) 0 );
        Arrays.fill( ciphertext, (byte) 0 );

        // O envelope deve ter mantido cópias intactas
        assertThat( envelope.wrappedDataKey() )
                .isEqualTo( wrappedDataKeyCopy );
        assertThat( envelope.iv() )
                .isEqualTo( ivCopy );
        assertThat( envelope.ciphertext() )
                .isEqualTo( ciphertextCopy );
    }

    @Test
    @DisplayName( "valores retornados são clones (defensive copy)" )
    void shouldReturnClonesNotReferences() {
        byte[] wrappedDataKey = generateRandomBytes( 64 );
        byte[] iv = generateRandomBytes( 12 );
        byte[] ciphertext = generateRandomBytes( 100 );

        EncryptedEnvelope envelope = new EncryptedEnvelope(
                1,
                "AES-256-GCM",
                "key-2026",
                wrappedDataKey,
                iv,
                ciphertext
        );

        byte[] retrievedWrappedKey = envelope.wrappedDataKey();
        byte[] retrievedIv = envelope.iv();
        byte[] retrievedCiphertext = envelope.ciphertext();

        // Modifica os arrays recuperados
        Arrays.fill( retrievedWrappedKey, (byte) 0 );
        Arrays.fill( retrievedIv, (byte) 0 );
        Arrays.fill( retrievedCiphertext, (byte) 0 );

        // O envelope deve manter suas cópias intactas
        assertThat( envelope.wrappedDataKey() )
                .isNotEqualTo( retrievedWrappedKey );
        assertThat( envelope.iv() )
                .isNotEqualTo( retrievedIv );
        assertThat( envelope.ciphertext() )
                .isNotEqualTo( retrievedCiphertext );
    }

    @Test
    @DisplayName( "null wrappedDataKey → NullPointerException" )
    void shouldRejectNullWrappedDataKey() {
        assertThatThrownBy( () -> new EncryptedEnvelope(
                1,
                "AES-256-GCM",
                "key-2026",
                null,
                generateRandomBytes( 12 ),
                generateRandomBytes( 100 )
        ) )
                .isInstanceOf( NullPointerException.class );
    }

    @Test
    @DisplayName( "null iv → NullPointerException" )
    void shouldRejectNullIv() {
        assertThatThrownBy( () -> new EncryptedEnvelope(
                1,
                "AES-256-GCM",
                "key-2026",
                generateRandomBytes( 64 ),
                null,
                generateRandomBytes( 100 )
        ) )
                .isInstanceOf( NullPointerException.class );
    }

    @Test
    @DisplayName( "null ciphertext → NullPointerException" )
    void shouldRejectNullCiphertext() {
        assertThatThrownBy( () -> new EncryptedEnvelope(
                1,
                "AES-256-GCM",
                "key-2026",
                generateRandomBytes( 64 ),
                generateRandomBytes( 12 ),
                null
        ) )
                .isInstanceOf( NullPointerException.class );
    }

    @Test
    @DisplayName( "null algorithm → NullPointerException" )
    void shouldRejectNullAlgorithm() {
        assertThatThrownBy( () -> new EncryptedEnvelope(
                1,
                null,
                "key-2026",
                generateRandomBytes( 64 ),
                generateRandomBytes( 12 ),
                generateRandomBytes( 100 )
        ) )
                .isInstanceOf( NullPointerException.class );
    }

    @Test
    @DisplayName( "null keyId → NullPointerException" )
    void shouldRejectNullKeyId() {
        assertThatThrownBy( () -> new EncryptedEnvelope(
                1,
                "AES-256-GCM",
                null,
                generateRandomBytes( 64 ),
                generateRandomBytes( 12 ),
                generateRandomBytes( 100 )
        ) )
                .isInstanceOf( NullPointerException.class );
    }

    @Test
    @DisplayName( "record com mesmos valores são equals" )
    void shouldHaveCorrectEquals() {
        byte[] wrappedDataKey = generateRandomBytes( 64 );
        byte[] iv = generateRandomBytes( 12 );
        byte[] ciphertext = generateRandomBytes( 100 );

        EncryptedEnvelope envelope1 = new EncryptedEnvelope(
                1,
                "AES-256-GCM",
                "key-2026",
                wrappedDataKey.clone(),
                iv.clone(),
                ciphertext.clone()
        );
        EncryptedEnvelope envelope2 = new EncryptedEnvelope(
                1,
                "AES-256-GCM",
                "key-2026",
                wrappedDataKey.clone(),
                iv.clone(),
                ciphertext.clone()
        );

        assertThat( envelope1 ).isEqualTo( envelope2 );
    }

    @Test
    @DisplayName( "record com versão diferente não são equals" )
    void shouldNotBeEqualWithDifferentVersion() {
        byte[] wrappedDataKey = generateRandomBytes( 64 );
        byte[] iv = generateRandomBytes( 12 );
        byte[] ciphertext = generateRandomBytes( 100 );

        EncryptedEnvelope envelope1 = new EncryptedEnvelope(
                1,
                "AES-256-GCM",
                "key-2026",
                wrappedDataKey.clone(),
                iv.clone(),
                ciphertext.clone()
        );
        EncryptedEnvelope envelope2 = new EncryptedEnvelope(
                2,
                "AES-256-GCM",
                "key-2026",
                wrappedDataKey.clone(),
                iv.clone(),
                ciphertext.clone()
        );

        assertThat( envelope1 ).isNotEqualTo( envelope2 );
    }
}
