package com.psicogest.psicogest.domain.privacy;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;

/**
 * Contexto fornecido pela política para cálculo de prazos regulatórios.
 *
 * A política jurídica/operacional define o calendário aplicável; o domínio
 * não presume que todo prazo seja simplesmente uma quantidade de dias corridos.
 */
public record PrivacyRegulatoryContext(
        ZoneId timezone,
        Set<LocalDate> holidays
) {

    public PrivacyRegulatoryContext {
        Objects.requireNonNull(timezone, "timezone");
        Objects.requireNonNull(holidays, "holidays");
        holidays = Set.copyOf(holidays);
    }
}
