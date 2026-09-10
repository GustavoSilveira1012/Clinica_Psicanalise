package com.psicogest.psicogest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelServiceInvoiceRequest(

    @NotBlank
    String reasonCode,

    @NotBlank
    @Size(max = 2000)
    String justification

) {}
