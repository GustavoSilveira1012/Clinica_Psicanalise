package com.psicogest.psicogest.security.config;

import com.psicogest.psicogest.security.audit.AuditProperties;
import com.psicogest.psicogest.security.auth.mfa.MfaProperties;
import com.psicogest.psicogest.security.crypto.CryptoProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Refuse placeholder, missing, or reused clinical keys before production starts. */
@Component
@Profile("production")
public class ProductionKeyMaterialValidator {
    public ProductionKeyMaterialValidator(
            CryptoProperties crypto, AuditProperties audit, MfaProperties mfa) {
        validate(crypto, audit, mfa);
    }

    public static void validate(CryptoProperties crypto, AuditProperties audit, MfaProperties mfa) {
        if (crypto == null || !"secret-managed".equals(crypto.provider())
                || crypto.localKeys() == null || crypto.localKeys().isEmpty()
                || crypto.currentKeyId() == null
                || !crypto.localKeys().containsKey(crypto.currentKeyId())) {
            throw new IllegalStateException("Keyring clínico de produção incompleto");
        }
        if (audit == null || audit.keys() == null || audit.keys().isEmpty()
                || audit.currentKeyId() == null
                || !audit.keys().containsKey(audit.currentKeyId())) {
            throw new IllegalStateException("Keyring de auditoria de produção incompleto");
        }
        if (mfa == null) {
            throw new IllegalStateException("Chave MFA de produção ausente");
        }

        List<byte[]> decoded = new ArrayList<>();
        try {
            for (Map.Entry<String, String> entry : crypto.localKeys().entrySet()) {
                decoded.add(decode(entry.getKey(), entry.getValue()));
            }
            for (Map.Entry<String, String> entry : audit.keys().entrySet()) {
                decoded.add(decode(entry.getKey(), entry.getValue()));
            }
            decoded.add(decode("MFA", mfa.encryptionKey()));
            for (int i = 0; i < decoded.size(); i++) {
                for (int j = i + 1; j < decoded.size(); j++) {
                    if (Arrays.equals(decoded.get(i), decoded.get(j))) {
                        throw new IllegalStateException("Chaves de produção não podem ser reutilizadas");
                    }
                }
            }
        } finally {
            decoded.forEach(bytes -> Arrays.fill(bytes, (byte) 0));
        }
    }

    private static byte[] decode(String id, String encoded) {
        if (id == null || id.isBlank() || encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("Chave de produção ausente ou sem identificador");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Chave de produção não é Base64 válido", exception);
        }
        if (bytes.length != 32 || Arrays.equals(bytes, new byte[32])) {
            Arrays.fill(bytes, (byte) 0);
            throw new IllegalStateException("Chave de produção deve ter 256 bits e não pode ser vazia");
        }
        return bytes;
    }
}
