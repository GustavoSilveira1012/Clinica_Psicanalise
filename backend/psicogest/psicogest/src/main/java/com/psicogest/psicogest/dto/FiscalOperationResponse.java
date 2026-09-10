package com.psicogest.psicogest.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FiscalOperationResponse {

    private UUID id;

    private UUID invoiceId;

    private String operationType;

    private String status;

    private String idempotencyKey;

    private Instant requestedAt;

    private Instant processedAt;

    private Instant completedAt;
}
