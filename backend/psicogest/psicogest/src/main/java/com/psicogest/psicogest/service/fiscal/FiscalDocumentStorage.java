package com.psicogest.psicogest.service.fiscal;

import java.io.InputStream;
import java.util.UUID;

public interface FiscalDocumentStorage {

    StoredFiscalDocument store(
        UUID invoiceId,
        FiscalDocumentType type,
        byte[] data,
        String contentType
    );

    InputStream open(
        String storageKey
    );
}
