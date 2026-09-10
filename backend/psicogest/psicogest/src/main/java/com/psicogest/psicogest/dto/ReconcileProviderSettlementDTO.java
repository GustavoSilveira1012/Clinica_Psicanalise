package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para reconciliação de lançamento bancário com ProviderSettlement
 */
public record ReconcileProviderSettlementDTO(

    /**
     * ID do repasse de gateway
     */
    @NotNull(message = "settlementId obrigatório")
    UUID settlementId,

    /**
     * Valor a alocar (pode ser parcial do settlement)
     */
    @NotNull(message = "amount obrigatório")
    @DecimalMin(value = "0.01", message = "amount deve ser positivo")
    BigDecimal amount

) {}
