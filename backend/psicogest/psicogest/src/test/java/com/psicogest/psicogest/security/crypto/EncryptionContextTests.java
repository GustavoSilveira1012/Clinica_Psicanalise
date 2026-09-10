package com.psicogest.psicogest.security.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName( "EncryptionContext" )
public class EncryptionContextTests {

    @Test
    @DisplayName( "AAD é canonicalizado corretamente com attributes" )
    void shouldGenerateCanonicalAad() {
        String resourceId = UUID.randomUUID().toString();
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                "content",
                Map.of("patientId", "100")
        );

        byte[] aad = context.aad();
        String aadString = new String( aad );

        assertThat( aadString )
                .contains( "PSICOGEST-ENCRYPTION-V1" )
                .contains( "resourceType=MEDICAL_RECORD" )
                .contains( "resourceId=" + resourceId )
                .contains( "patientId=100" )
                .contains( "fieldName=content" );
    }

    @Test
    @DisplayName( "AAD com webhook provider" )
    void shouldGenerateAadForWebhook() {
        String inboxId = UUID.randomUUID().toString();
        EncryptionContext context = new EncryptionContext(
                "PAYMENT_WEBHOOK",
                inboxId,
                "payload",
                Map.of(
                        "provider", "STRIPE",
                        "eventId", "evt_ABC123"
                )
        );

        byte[] aad = context.aad();
        String aadString = new String( aad );

        assertThat( aadString )
                .contains( "PSICOGEST-ENCRYPTION-V1" )
                .contains( "resourceType=PAYMENT_WEBHOOK" )
                .contains( "resourceId=" + inboxId )
                .contains( "fieldName=payload" )
                .contains( "provider=STRIPE" )
                .contains( "eventId=evt_ABC123" );
    }

    @Test
    @DisplayName( "keyManagementContext contém todos os campos" )
    void shouldGenerateCompleteKeyManagementContext() {
        String resourceId = UUID.randomUUID().toString();
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                "content",
                Map.of("patientId", "100")
        );

        Map<String, String> kmsContext = context.keyManagementContext();

        assertThat( kmsContext )
                .containsEntry( "application", "PsicoGest" )
                .containsEntry( "resourceType", "MEDICAL_RECORD" )
                .containsEntry( "resourceId", resourceId )
                .containsEntry( "patientId", "100" )
                .containsEntry( "fieldName", "content" );
    }

    @Test
    @DisplayName( "AAD muda quando attributes mudam" )
    void shouldProduceDifferentAadForDifferentAttributes() {
        String resourceId = UUID.randomUUID().toString();
        EncryptionContext context1 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                "content",
                Map.of("patientId", "100")
        );
        EncryptionContext context2 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                "content",
                Map.of("patientId", "200")
        );

        assertThat( context1.aad() )
                .isNotEqualTo( context2.aad() );
    }

    @Test
    @DisplayName( "AAD muda quando resourceId muda" )
    void shouldProduceDifferentAadForDifferentResources() {
        EncryptionContext context1 = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                "content",
                Map.of("patientId", "100")
        );
        EncryptionContext context2 = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                "content",
                Map.of("patientId", "100")
        );

        assertThat( context1.aad() )
                .isNotEqualTo( context2.aad() );
    }

    @Test
    @DisplayName( "AAD muda quando fieldName muda" )
    void shouldProduceDifferentAadForDifferentFields() {
        String resourceId = UUID.randomUUID().toString();
        EncryptionContext context1 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                "content",
                Map.of("patientId", "100")
        );
        EncryptionContext context2 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                "addendum",
                Map.of("patientId", "100")
        );

        assertThat( context1.aad() )
                .isNotEqualTo( context2.aad() );
    }

    @Test
    @DisplayName( "resourceType null → NullPointerException" )
    void shouldRejectNullResourceType() {
        assertThatThrownBy( () -> new EncryptionContext(
                null,
                UUID.randomUUID().toString(),
                "content",
                Map.of()
        ) )
                .isInstanceOf( NullPointerException.class );
    }

    @Test
    @DisplayName( "resourceId null → NullPointerException" )
    void shouldRejectNullResourceId() {
        assertThatThrownBy( () -> new EncryptionContext(
                "MEDICAL_RECORD",
                null,
                "content",
                Map.of()
        ) )
                .isInstanceOf( NullPointerException.class );
    }

    @Test
    @DisplayName( "fieldName null → NullPointerException" )
    void shouldRejectNullFieldName() {
        assertThatThrownBy( () -> new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                null,
                Map.of()
        ) )
                .isInstanceOf( NullPointerException.class );
    }

    @Test
    @DisplayName( "attributes null → convertido para empty Map" )
    void shouldConvertNullAttributesToEmptyMap() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                "content",
                null
        );

        assertThat( context.attributes() )
                .isEmpty();
    }

    @Test
    @DisplayName( "AAD é determinístico" )
    void shouldGenerateDeterministicAad() {
        String resourceId = UUID.randomUUID().toString();
        EncryptionContext context1 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                "content",
                Map.of("patientId", "100")
        );
        EncryptionContext context2 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                "content",
                Map.of("patientId", "100")
        );

        assertThat( context1.aad() )
                .isEqualTo( context2.aad() );
    }

    @Test
    @DisplayName( "attributes são ordenados lexicograficamente na AAD" )
    void shouldSortAttributesLexicographicallyInAad() {
        String resourceId = UUID.randomUUID().toString();
        
        // Criar com ordem diferente
        Map<String, String> attrs1 = Map.of(
                "zebra", "1",
                "apple", "2",
                "monkey", "3"
        );
        
        Map<String, String> attrs2 = Map.of(
                "apple", "2",
                "monkey", "3",
                "zebra", "1"
        );
        
        EncryptionContext context1 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                "content",
                attrs1
        );
        
        EncryptionContext context2 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                "content",
                attrs2
        );

        // AAD deve ser idêntico (ordenado)
        assertThat( context1.aad() )
                .isEqualTo( context2.aad() );
    }

    @Test
    @DisplayName( "keyManagementContext é imutável" )
    void shouldReturnImmutableKeyManagementContext() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                "content",
                Map.of("patientId", "100")
        );

        Map<String, String> kmsContext = context.keyManagementContext();

        assertThatThrownBy( () -> kmsContext.put( "fake", "value" ) )
                .isInstanceOf( UnsupportedOperationException.class );
    }
}
