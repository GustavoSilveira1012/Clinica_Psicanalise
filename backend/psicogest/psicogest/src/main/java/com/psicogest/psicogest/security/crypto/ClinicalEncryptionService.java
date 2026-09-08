package com.psicogest.psicogest.security.crypto;

import org.springframework.stereotype.Service;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

@Service
public class ClinicalEncryptionService {

    private static final String ALGORITHM = "AES-256-GCM";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int CRYPTO_VERSION = 1;
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final DataKeyProvider keyProvider;

    public ClinicalEncryptionService(
            DataKeyProvider keyProvider
    ) {
        this.keyProvider = keyProvider;
    }

    public EncryptedEnvelope encrypt(
            String plaintext,
            EncryptionContext context
    ) {
        if ( plaintext == null ) {
            throw new IllegalArgumentException(
                    "Conteúdo clínico não pode ser null"
            );
        }
        try ( GeneratedDataKey generatedKey = keyProvider
                .generateDataKey( context ) ) {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes( iv );
            Cipher cipher = Cipher.getInstance( CIPHER );
            SecretKeySpec key = new SecretKeySpec(
                    generatedKey.plaintextKey(),
                    "AES"
            );
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    new GCMParameterSpec( TAG_BITS, iv )
            );
            cipher.updateAAD( context.aad() );
            byte[] ciphertext = cipher.doFinal(
                    plaintext.getBytes( StandardCharsets.UTF_8 )
            );
            return new EncryptedEnvelope(
                    CRYPTO_VERSION,
                    ALGORITHM,
                    generatedKey.keyId(),
                    generatedKey.wrappedKey(),
                    iv,
                    ciphertext
            );
        } catch ( ClinicalEncryptionException exception ) {
            throw exception;
        } catch ( Exception exception ) {
            throw new ClinicalEncryptionException(
                    "Falha ao proteger conteúdo clínico",
                    exception
            );
        }
    }

    public String decrypt(
            EncryptedEnvelope envelope,
            EncryptionContext context
    ) {
        validateEnvelope( envelope );
        byte[] rawDataKey = keyProvider.decryptDataKey(
                envelope.keyId(),
                envelope.wrappedDataKey(),
                context
        );
        try {
            Cipher cipher = Cipher.getInstance( CIPHER );
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec( rawDataKey, "AES" ),
                    new GCMParameterSpec( TAG_BITS, envelope.iv() )
            );
            cipher.updateAAD( context.aad() );
            byte[] plaintext = cipher.doFinal( envelope.ciphertext() );
            try {
                return new String( plaintext, StandardCharsets.UTF_8 );
            } finally {
                Arrays.fill( plaintext, (byte) 0 );
            }
        } catch ( AEADBadTagException exception ) {
            throw new ClinicalDataIntegrityException(
                    "Falha de integridade do conteúdo clínico",
                    exception
            );
        } catch ( Exception exception ) {
            throw new ClinicalEncryptionException(
                    "Falha ao descriptografar conteúdo clínico",
                    exception
            );
        } finally {
            Arrays.fill( rawDataKey, (byte) 0 );
        }
    }

    private void validateEnvelope(
            EncryptedEnvelope envelope
    ) {
        if ( envelope.cryptoVersion() != CRYPTO_VERSION ) {
            throw new ClinicalEncryptionException(
                    "Versão criptográfica não suportada"
            );
        }
        if ( !ALGORITHM.equals( envelope.algorithm() ) ) {
            throw new ClinicalEncryptionException(
                    "Algoritmo criptográfico não suportado"
            );
        }
        if ( envelope.iv().length != IV_LENGTH ) {
            throw new ClinicalEncryptionException(
                    "IV criptográfico inválido"
            );
        }
    }
}
