package com.psicogest.psicogest.security.config;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Runtime credentials must not bypass tenant policies or alter the schema. */
@Component
@Profile("production")
public class ProductionDatabaseRoleValidator implements SmartInitializingSingleton {
    private final JdbcTemplate jdbc;
    public ProductionDatabaseRoleValidator(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void afterSingletonsInstantiated() {
        Boolean unsafe = jdbc.queryForObject("""
                SELECT rolsuper OR rolbypassrls OR rolcreatedb OR rolcreaterole
                    OR rolreplication OR current_setting('row_security') <> 'on'
                    OR EXISTS (
                        SELECT 1 FROM pg_namespace n
                        WHERE n.nspname IN ('public', 'app')
                          AND has_schema_privilege(current_user, n.oid, 'CREATE')
                    )
                    OR EXISTS (
                        SELECT 1 FROM pg_class c
                        JOIN pg_namespace n ON n.oid = c.relnamespace
                        WHERE n.nspname IN ('public', 'app')
                          AND c.relkind IN ('r', 'p', 'v', 'm', 'S')
                          AND pg_has_role(current_user, c.relowner, 'MEMBER')
                    )
                  FROM pg_roles WHERE rolname = current_user
                """, Boolean.class);
        if (!Boolean.FALSE.equals(unsafe)) {
            throw new IllegalStateException("Credencial de runtime não pode ignorar RLS ou alterar o schema; use credenciais separadas para migrations");
        }
    }
}
