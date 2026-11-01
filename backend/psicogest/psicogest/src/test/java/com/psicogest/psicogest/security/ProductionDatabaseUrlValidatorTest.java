package com.psicogest.psicogest.security;

import com.psicogest.psicogest.security.config.ProductionDatabaseUrlValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionDatabaseUrlValidatorTest {

    @Test
    void acceptsPostgresUrlWithFullTlsVerificationAndSeparateCredentials() {
        assertThatCode(() -> ProductionDatabaseUrlValidator.validate(
                "jdbc:postgresql://db.synthetic-ref.supabase.co:5432/postgres?sslmode=verify-full"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAdditionalParametersThatCanOverrideTlsOrConnectionTarget() {
        for (String suffix : new String[]{
                "&sslmode=disable",
                "&ssl=true",
                "&sslfactory=org.postgresql.ssl.NonValidatingFactory",
                "&host=other.example.invalid",
                "&port=5433",
                "&connectTimeout=5",
                "#fragment"
        }) {
            assertThatThrownBy(() -> ProductionDatabaseUrlValidator.validate(
                    "jdbc:postgresql://db.synthetic-ref.supabase.co:5432/postgres?sslmode=verify-full" + suffix))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("sslmode=verify-full");
        }
    }

    @Test
    void rejectsMissingOrWeakTlsMode() {
        for (String url : new String[]{
                "jdbc:postgresql://db.synthetic-ref.supabase.co:5432/postgres",
                "jdbc:postgresql://db.synthetic-ref.supabase.co:5432/postgres?sslmode=require",
                "jdbc:postgresql://db.synthetic-ref.supabase.co:5432/postgres?sslmode=prefer"
        }) {
            assertThatThrownBy(() -> ProductionDatabaseUrlValidator.validate(url))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("sslmode=verify-full");
        }
    }

    @Test
    void rejectsUrlsWithEmbeddedCredentialsOrMissingDatabaseHost() {
        for (String url : new String[]{
                "jdbc:postgresql://postgres:password@db.synthetic-ref.supabase.co:5432/postgres?sslmode=verify-full",
                "jdbc:postgresql:///postgres?sslmode=verify-full",
                "jdbc:mysql://db.synthetic-ref.supabase.co:5432/postgres?sslmode=verify-full",
                ""
        }) {
            assertThatThrownBy(() -> ProductionDatabaseUrlValidator.validate(url))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
