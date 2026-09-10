package com.psicogest.psicogest.service.fiscal;

import java.util.UUID;

public record FiscalSubstitutionCommand(

    UUID originalInvoiceId,

    UUID newInvoiceId,

    String originalNfseId,

    long newDpsNumber

) {}
