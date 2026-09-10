package com.psicogest.psicogest.service.fiscal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record MunicipalFiscalParameters(

    String sourceVersion,

    BigDecimal issRate,

    boolean issWithheld,

    String specialTaxRegime,

    Map<String, Object> additionalParameters,

    String responseHash,

    Instant fetchedAt

) {}
