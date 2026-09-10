package com.psicogest.psicogest.service.fiscal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FiscalIssueCommand(

    UUID issuerId,

    UUID invoiceId,

    long dpsNumber,

    String layoutVersion,

    LocalDate competenceDate,

    BigDecimal grossAmount,

    BigDecimal deductions,

    BigDecimal netAmount,

    String serviceDescription

) {}
