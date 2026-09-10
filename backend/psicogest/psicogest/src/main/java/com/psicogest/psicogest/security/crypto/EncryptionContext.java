package com.psicogest.psicogest.security.crypto;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contexto de criptografia genérico
 * 
 * 14. Refatorar EncryptionContext
 * 
 * Antes: específico para clínica com patientId obrigatório
 * Agora: genérico com attributes map
 * 
 * Para MedicalRecord:
 *   new EncryptionContext(
 *     "MEDICAL_RECORD",
 *     recordId.toString(),
 *     "content",
 *     Map.of("patientId", patientId.toString())
 *   )
 * 
 * Para Webhook:
 *   new EncryptionContext(
 *     "PAYMENT_WEBHOOK",
 *     inboxId.toString(),
 *     "payload",
 *     Map.of("provider", provider.name())
 *   )
 */
public record EncryptionContext(

        /**
         * Tipo de recurso (MEDICAL_RECORD, PAYMENT_WEBHOOK, etc)
         */
        String resourceType,

        /**
         * ID do recurso
         */
        String resourceId,

        /**
         * Nome do campo criptografado
         */
        String fieldName,

        /**
         * Atributos adicionais (patientId, provider, etc)
         * Será usado na AAD canônica
         */
        Map<String, String> attributes

) {

    /**
     * Compact constructor com validação
     */
    public EncryptionContext {

        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(fieldName, "fieldName");

        attributes = attributes != null
                ? Map.copyOf(attributes)
                : Map.of();
    }

    /**
     * 15. AAD canônica
     * 
     * Inclui attributes ordenados lexicograficamente
     */
    public byte[] aad() {

        StringBuilder builder =
                new StringBuilder();

        builder.append("PSICOGEST-ENCRYPTION-V1\n");

        builder.append("resourceType=")
                .append(resourceType)
                .append('\n');

        builder.append("resourceId=")
                .append(resourceId)
                .append('\n');

        builder.append("fieldName=")
                .append(fieldName)
                .append('\n');

        // Atributos ordenados lexicograficamente
        attributes.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                        entry -> builder
                                .append(entry.getKey())
                                .append('=')
                                .append(entry.getValue())
                                .append('\n')
                );

        return builder
                .toString()
                .getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Contexto para gerenciamento de chaves
     */
    public Map<String, String> keyManagementContext() {

        Map<String, String> context =
                new LinkedHashMap<>();

        context.put("application", "PsicoGest");
        context.put("resourceType", resourceType);
        context.put("resourceId", resourceId);
        context.put("fieldName", fieldName);

        // Adiciona atributos
        context.putAll(attributes);

        return Map.copyOf(context);
    }
}
