package com.psicogest.psicogest.service.fiscal;

public record FiscalCancellationResult(

    boolean success,

    String status,

    String errorMessage

) {}
