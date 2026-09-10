package com.psicogest.psicogest.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record SubstituteServiceInvoiceRequest(

    @NotNull
    UUID newInvoiceId

) {}
