package com.psicogest.psicogest.security.crypto;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record EncryptionContext(
        String resourceType,
        String resourceId,
        Long patientId,
        String fieldName
) {
    public EncryptionContext {
        Objects.requireNonNull( resourceType, "resourceType" );
        Objects.requireNonNull( resourceId, "resourceId" );
        Objects.requireNonNull( patientId, "patientId" );
        Objects.requireNonNull( fieldName, "fieldName" );
    }

    public byte[] aad() {
        String canonical = String.join(
                "\n",
                "PSICOGEST-CLINICAL-V1",
                "resourceType=" + resourceType,
                "resourceId=" + resourceId,
                "patientId=" + patientId,
                "fieldName=" + fieldName
        );
        return canonical.getBytes( StandardCharsets.UTF_8 );
    }

    public Map<String, String> keyManagementContext() {
        Map<String, String> context = new LinkedHashMap<>();
        context.put( "application", "PsicoGest" );
        context.put( "resourceType", resourceType );
        context.put( "resourceId", resourceId );
        context.put( "patientId", patientId.toString() );
        context.put( "fieldName", fieldName );
        return Map.copyOf( context );
    }
}
