package com.psicogest.psicogest.security.tenant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Sets transaction-local PostgreSQL context used by RLS policies. */
@Component
public class TenantDatabaseContext {

    private final JdbcTemplate jdbcTemplate;

    public TenantDatabaseContext(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void applyUser(Long userId) {
        if (userId != null) {
            jdbcTemplate.queryForObject(
                    "select set_config('app.user_id', ?, true)",
                    String.class,
                    userId.toString());
        }
    }

    public void applyOrganization(UUID organizationId) {
        if (organizationId != null) {
            jdbcTemplate.queryForObject(
                    "select set_config('app.organization_id', ?, true)",
                    String.class,
                    organizationId.toString());
        }
    }

    public void applyInviteToken(String tokenHash) {
        if (tokenHash != null && !tokenHash.isBlank()) {
            jdbcTemplate.queryForObject(
                    "select set_config('app.invite_token_hash', ?, true)",
                    String.class,
                    tokenHash);
        }
    }
}
