package com.psicogest.psicogest.service.fiscal;

public record FiscalSubstitutionResult(

    boolean success,

    String newNfseId,

    String newAccessKey,

    String originalNfseStatus,

    String errorMessage

) {}
