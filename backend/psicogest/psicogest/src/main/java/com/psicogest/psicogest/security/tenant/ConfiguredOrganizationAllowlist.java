package com.psicogest.psicogest.security.tenant;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Strict parser for operator-managed background-job tenant allowlists. */
public final class ConfiguredOrganizationAllowlist {

    private static final int MAX_ORGANIZATIONS = 100;

    private ConfiguredOrganizationAllowlist() { }

    public static List<UUID> parse(String configuredIds) {
        if (configuredIds == null || configuredIds.isBlank()) {
            return List.of();
        }
        List<UUID> ids = Arrays.stream(configuredIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(ConfiguredOrganizationAllowlist::parseCanonicalUuid)
                .distinct()
                .toList();
        if (ids.size() > MAX_ORGANIZATIONS) {
            throw new IllegalArgumentException("Allowlist de background excede o limite de organizações");
        }
        return ids;
    }

    private static UUID parseCanonicalUuid(String value) {
        try {
            UUID parsed = UUID.fromString(value);
            if (!parsed.toString().equalsIgnoreCase(value)) {
                throw new IllegalArgumentException("UUID não canônico");
            }
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Allowlist de background contém UUID inválido", exception);
        }
    }
}
