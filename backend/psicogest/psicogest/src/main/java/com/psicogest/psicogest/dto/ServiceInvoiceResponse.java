package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceInvoiceResponse {

    private UUID id;

    private UUID clinicId;

    private String provider;

    private String environment;

    private String taxRegime;

    private String status;

    private String invoiceNumber;

    private String nfseId;

    private BigDecimal grossAmount;

    private BigDecimal deductions;

    private BigDecimal netAmount;

    private String currency;

    private Instant createdAt;

    private Instant submittedAt;

    private Instant authorizedAt;

    private Instant rejectedAt;

    private Instant cancelledAt;

    private Instant updatedAt;

    private String rejectionReason;
}
