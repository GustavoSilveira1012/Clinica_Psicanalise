package com.psicogest.psicogest.security.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psicogest.psicogest.model.entity.AuditLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Service
public class AuditIntegrityService {

    private final ObjectMapper mapper;

    private final AuditKeyProvider keyProvider;

    public AuditIntegrityService(
            @Qualifier("auditObjectMapper")
            ObjectMapper mapper,
            AuditKeyProvider keyProvider
    ) {

        this.mapper = mapper;
        this.keyProvider = keyProvider;
    }

    public String metadataHash(
            Map<String, Object> metadata
    ) {

        try {

            String canonical =
                    mapper.writeValueAsString(
                            metadata != null
                                    ? metadata
                                    : Map.of()
                    );

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            return HexFormat.of()
                    .formatHex(
                            digest.digest(
                                    canonical.getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            )
                    );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Falha ao gerar hash de auditoria",
                    exception
            );
        }
    }

    public String calculateMac(
            AuditLog audit,
            SecretKey key
    ) {

        try {

            String payload =
                    canonicalPayload(
                            audit
                    );

            Mac mac =
                    Mac.getInstance(
                            "HmacSHA256"
                    );

            mac.init(key);

            byte[] result =
                    mac.doFinal(
                            payload.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(result);

        } catch (
                GeneralSecurityException exception
        ) {

            throw new IllegalStateException(
                    "Falha na integridade da auditoria",
                    exception
            );
        }
    }

    private String canonicalPayload(
            AuditLog audit
    ) {

        return String.join(
                "\n",

                "PSICOGEST-AUDIT-V1",

                String.valueOf(
                        audit.getSequence()
                ),

                audit.getId().toString(),

                value(audit.getPreviousMac()),

                value(
                        audit.getActorUser() != null
                                ? audit.getActorUser()
                                        .getId()
                                        .toString()
                                : null
                ),

                value(
                        audit.getSession() != null
                                ? audit.getSession()
                                        .getId()
                                        .toString()
                                : null
                ),

                audit.getAction().name(),

                value(audit.getResourceType()),

                value(audit.getResourceId()),

                value(audit.getPatientId()),

                value(audit.getClinicContextId()),

                audit.getOutcome().name(),

                audit.getOccurredAt()
                        .toString(),

                value(audit.getCorrelationId()),

                value(audit.getSourceIp()),

                value(audit.getUserAgentHash()),

                audit.getMetadataHash(),

                audit.getKeyId()
        );
    }

    private String value(
            Object value
    ) {

        return value == null
                ? ""
                : value.toString();
    }
}
