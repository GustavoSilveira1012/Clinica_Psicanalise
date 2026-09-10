package com.psicogest.psicogest.service.fiscal;

import java.time.Instant;

public record StoredFiscalDocument(

    String storageKey,

    String contentType,

    long fileSize,

    String sha256Hash,

    Instant storedAt

) {}
