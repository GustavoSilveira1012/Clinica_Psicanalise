package com.psicogest.psicogest.service.fiscal;

import java.util.Optional;

import com.psicogest.psicogest.model.entity.ServiceInvoice;

public interface FiscalDocumentProvider {

    Optional<byte[]> fetchDanfse(
        ServiceInvoice invoice
    );
}
