package com.psicogest.psicogest.service.fiscal;

import java.util.UUID;

public record FiscalCancellationCommand(

    UUID invoiceId,

    String nfseId,

    String reasonCode,

    String justification

) {}
