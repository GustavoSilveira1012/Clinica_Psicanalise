package com.psicogest.psicogest.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceInvoiceCreateRequest(

    @NotNull
    UUID fiscalIssuerId,

    @NotNull
    LocalDate competenceDate,

    @NotEmpty
    List<InvoiceOriginRequest> origins,

    @NotNull
    @Valid
    FiscalTakerRequest taker,

    @NotBlank
    @Size(max = 2000)
    String serviceDescription

) {}
