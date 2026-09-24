package com.psicogest.psicogest.security.config;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Runtime credentials must not bypass PostgreSQL tenant policies. */
@Component
@Profile("production")
public class ProductionDatabaseRoleValidator implements SmartInitializingSingleton {
    private final JdbcTemplate jdbc;
    public ProductionDatabaseRoleValidator(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void afterSingletonsInstantiated() {
        Boolean unsafe = jdbc.queryForObject("""
                SELECT rolsuper OR rolbypassrls OR current_setting('row_security') <> 'on'
                  FROM pg_roles WHERE rolname = current_user
                """, Boolean.class);
        if (!Boolean.FALSE.equals(unsafe)) {
            throw new IllegalStateException("Credencial de runtime não pode ser superuser ou BYPASSRLS; use credenciais separadas para migrations");
        }
    }
}
