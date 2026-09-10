package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record InvoiceOriginRequest(

    @NotNull
    UUID receivableId,

    @NotNull
    @DecimalMin(value = "0.01")
    BigDecimal amount

) {}
