package com.psicogest.psicogest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FiscalTakerRequest(

    @NotBlank
    @Size(min = 11, max = 14)
    String taxId,

    @NotBlank
    @Size(max = 255)
    String name,

    @Size(max = 255)
    String address,

    @Size(max = 50)
    String city,

    @Size(max = 2)
    String state,

    @Size(max = 10)
    String zipCode

) {}
