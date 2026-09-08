package com.psicogest.psicogest.security.audit;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class PropertyAuditKeyProvider
        implements AuditKeyProvider {

    private final AuditProperties properties;

    public PropertyAuditKeyProvider(
            AuditProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    public String currentKeyId() {

        return properties.currentKeyId();
    }

    @Override
    public SecretKey currentKey() {

        return keyFor(
                currentKeyId()
        );
    }

    @Override
    public SecretKey keyFor(
            String keyId
    ) {

        String encoded =
                properties.keys()
                        .get(keyId);

        if (encoded == null) {

            throw new IllegalStateException(
                    "Chave de auditoria não disponível: "
                    + keyId
            );
        }

        byte[] raw =
                Base64.getDecoder()
                        .decode(encoded);

        if (raw.length < 32) {

            throw new IllegalStateException(
                    "Chave de auditoria deve possuir pelo menos 256 bits"
            );
        }

        return new SecretKeySpec(
                raw,
                "HmacSHA256"
        );
    }
}
