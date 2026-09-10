CREATE TABLE fiscal_documents (

    id UUID PRIMARY KEY,

    invoice_id UUID NOT NULL,

    document_type VARCHAR(40) NOT NULL,

    storage_key VARCHAR(500) NOT NULL,

    sha256 VARCHAR(64) NOT NULL,

    content_type VARCHAR(100) NOT NULL,

    size_bytes BIGINT NOT NULL,

    layout_version VARCHAR(50),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_fiscal_document_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES service_invoices(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_fiscal_document_type
        CHECK (
            document_type IN (
                'DPS_XML',
                'NFSE_XML',
                'DANFSE_PDF',
                'EVENT_XML'
            )
        )
);