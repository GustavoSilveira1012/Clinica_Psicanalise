package com.psicogest.psicogest.service.fiscal;

public record FiscalIssueResult(

    boolean success,

    String nfseId,

    String accessKey,

    String errorMessage

) {}
