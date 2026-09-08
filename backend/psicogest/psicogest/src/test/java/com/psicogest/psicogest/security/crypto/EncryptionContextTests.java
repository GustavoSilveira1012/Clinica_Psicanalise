package com.psicogest.psicogest.security.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName( "EncryptionContext" )
public class EncryptionContextTests {

    @Test
    @DisplayName( "AAD é canonicalizado corretamente" )
    void shouldGenerateCanonicalAad() {
        String resourceId = UUID.randomUUID().toString();
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                100L,
                "content"
        );

        byte[] aad = context.aad();
        String aadString = new String( aad );

        assertThat( aadString )
                .contains( "PSICOGEST-CLINICAL-V1" )
                .contains( "resourceType=MEDICAL_RECORD" )
                .contains( "resourceId=" + resourceId )
                .contains( "patientId=100" )
                .contains( "fieldName=content" );
    }

    @Test
    @DisplayName( "keyManagementContext contém todos os campos" )
    void shouldGenerateCompleteKeyManagementContext() {
        String resourceId = UUID.randomUUID().toString();
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                100L,
                "content"
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
    @DisplayName( "AAD muda quando patientId muda" )
    void shouldProduceDifferentAadForDifferentPatients() {
        String resourceId = UUID.randomUUID().toString();
        EncryptionContext context1 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                100L,
                "content"
        );
        EncryptionContext context2 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                200L,
                "content"
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
                100L,
                "content"
        );
        EncryptionContext context2 = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
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
                100L,
                "content"
        );
        EncryptionContext context2 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                100L,
                "addendum"
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
                100L,
                "content"
        ) )
                .isInstanceOf( NullPointerException.class );
    }

    @Test
    @DisplayName( "resourceId null → NullPointerException" )
    void shouldRejectNullResourceId() {
        assertThatThrownBy( () -> new EncryptionContext(
                "MEDICAL_RECORD",
                null,
                100L,
                "content"
        ) )
                .isInstanceOf( NullPointerException.class );
    }

    @Test
    @DisplayName( "patientId null → NullPointerException" )
    void shouldRejectNullPatientId() {
        assertThatThrownBy( () -> new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                null,
                "content"
        ) )
                .isInstanceOf( NullPointerException.class );
    }

    @Test
    @DisplayName( "fieldName null → NullPointerException" )
    void shouldRejectNullFieldName() {
        assertThatThrownBy( () -> new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                null
        ) )
                .isInstanceOf( NullPointerException.class );
    }

    @Test
    @DisplayName( "AAD é determinístico" )
    void shouldGenerateDeterministicAad() {
        String resourceId = UUID.randomUUID().toString();
        EncryptionContext context1 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                100L,
                "content"
        );
        EncryptionContext context2 = new EncryptionContext(
                "MEDICAL_RECORD",
                resourceId,
                100L,
                "content"
        );

        assertThat( context1.aad() )
                .isEqualTo( context2.aad() );
    }

    @Test
    @DisplayName( "keyManagementContext é imutável" )
    void shouldReturnImmutableKeyManagementContext() {
        EncryptionContext context = new EncryptionContext(
                "MEDICAL_RECORD",
                UUID.randomUUID().toString(),
                100L,
                "content"
        );

        Map<String, String> kmsContext = context.keyManagementContext();

        assertThatThrownBy( () -> kmsContext.put( "fake", "value" ) )
                .isInstanceOf( UnsupportedOperationException.class );
    }
}
