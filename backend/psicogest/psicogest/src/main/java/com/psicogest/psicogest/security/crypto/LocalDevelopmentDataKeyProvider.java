package com.psicogest.psicogest.security.crypto;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
@Profile( "!prod" )
@ConditionalOnProperty(
        name = "app.security.crypto.provider",
        havingValue = "local"
)
public class LocalDevelopmentDataKeyProvider implements DataKeyProvider {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CryptoProperties properties;

    public LocalDevelopmentDataKeyProvider(
            CryptoProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    public GeneratedDataKey generateDataKey(
            EncryptionContext context
    ) {
        try {
            KeyGenerator generator = KeyGenerator.getInstance( "AES" );
            generator.init( 256, RANDOM );
            SecretKey dataKey = generator.generateKey();
            byte[] rawDataKey = dataKey.getEncoded();
            String keyId = properties.currentKeyId();
            SecretKey wrappingKey = resolveWrappingKey( keyId );
            byte[] wrapped = wrap( rawDataKey, wrappingKey, keyId, context );
            try {
                return new GeneratedDataKey( rawDataKey, wrapped, keyId );
            } finally {
                Arrays.fill( rawDataKey, (byte) 0 );
            }
        } catch ( Exception exception ) {
            throw new ClinicalEncryptionException(
                    "Falha ao gerar chave de dados",
                    exception
            );
        }
    }

    @Override
    public byte[] decryptDataKey(
            String keyId,
            byte[] wrappedDataKey,
            EncryptionContext context
    ) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap( wrappedDataKey );
            byte version = buffer.get();
            if ( version != 1 ) {
                throw new ClinicalEncryptionException(
                        "Versão de envelope não suportada"
                );
            }
            byte[] iv = new byte[IV_LENGTH];
            buffer.get( iv );
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get( ciphertext );

            Cipher cipher = Cipher.getInstance( "AES/GCM/NoPadding" );
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    resolveWrappingKey( keyId ),
                    new GCMParameterSpec( TAG_BITS, iv )
            );
            cipher.updateAAD( wrappingAad( keyId, context ) );
            return cipher.doFinal( ciphertext );
        } catch ( javax.crypto.AEADBadTagException exception ) {
            throw new ClinicalDataIntegrityException(
                    "Integridade da chave de dados inválida",
                    exception
            );
        } catch ( Exception exception ) {
            throw new ClinicalEncryptionException(
                    "Falha ao abrir chave de dados",
                    exception
            );
        }
    }

    private SecretKey resolveWrappingKey( String keyId ) {
        String encoded = properties
                .localKeys()
                .get( keyId );
        if ( encoded == null ) {
            throw new IllegalStateException(
                    "Chave clínica não encontrada: " + keyId
            );
        }
        byte[] raw = Base64
                .getDecoder()
                .decode( encoded );
        if ( raw.length != 32 ) {
            throw new IllegalStateException(
                    "CLINICAL_LOCAL_KEK deve possuir 256 bits"
            );
        }
        return new SecretKeySpec( raw, "AES" );
    }

    private byte[] wrap(
            byte[] dataKey,
            SecretKey wrappingKey,
            String keyId,
            EncryptionContext context
    ) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes( iv );
        Cipher cipher = Cipher.getInstance( "AES/GCM/NoPadding" );
        cipher.init(
                Cipher.ENCRYPT_MODE,
                wrappingKey,
                new GCMParameterSpec( TAG_BITS, iv )
        );
        cipher.updateAAD( wrappingAad( keyId, context ) );
        byte[] encrypted = cipher.doFinal( dataKey );
        return ByteBuffer
                .allocate( 1 + IV_LENGTH + encrypted.length )
                .put( (byte) 1 )
                .put( iv )
                .put( encrypted )
                .array();
    }

    private byte[] wrappingAad(
            String keyId,
            EncryptionContext context
    ) {
        byte[] contextAad = context.aad();
        byte[] prefix = ( "PSICOGEST-DEK-WRAP-V1\n"
                + "keyId=" + keyId + "\n" )
                .getBytes( StandardCharsets.UTF_8 );
        return ByteBuffer
                .allocate( prefix.length + contextAad.length )
                .put( prefix )
                .put( contextAad )
                .array();
    }
}
