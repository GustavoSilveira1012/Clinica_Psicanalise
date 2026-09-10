package com.psicogest.psicogest.service.fiscal;

import java.math.BigDecimal;
import java.time.Instant;

public record FiscalQueryResult(

    String nfseId,

    String accessKey,

    String status,

    BigDecimal netAmount,

    Instant issuedAt

) {}
