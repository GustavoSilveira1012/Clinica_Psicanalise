package com.psicogest.psicogest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de resposta para ProviderSettlement
 * 
 * Contém totalizações de itens:
 * - grossPayments: soma de pagamentos
 * - grossRefunds: soma de refunds
 * - providerFees: soma de taxas
 * - chargebacks: soma de chargebacks
 * - signedNet: saldo assinado (créditos - débitos)
 * - reportedNet: saldo informado pelo gateway
 * - reconciled: saldo já conciliado com banco
 * - outstanding: saldo pendente de conciliação
 * - validationStatus: MATCHED, MISMATCHED, PENDING, REMATCH
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderSettlementResponseDTO {

    /**
     * ID do repasse
     */
    private UUID id;

    /**
     * ID da clínica
     */
    private Long clinicId;

    /**
     * Provedor (MERCADO_PAGO, STRIPE, etc)
     */
    private String provider;

    /**
     * ID do repasse no provedor
     */
    private String providerSettlementId;

    /**
     * Status: RECEIVED, VALIDATED, SETTLED, MISMATCHED
     */
    private String status;

    /**
     * Status de validação: PENDING, MATCHED, MISMATCHED, REMATCH
     */
    private String validationStatus;

    /**
     * Direção: CREDIT_TO_FINANCIAL_ENTITY ou DEBIT_FROM_FINANCIAL_ENTITY
     */
    private String direction;

    /**
     * Moeda
     */
    private String currency;

    /**
     * Timestamps
     */
    private Instant expectedAt;

    private Instant settledAt;

    private Instant validatedAt;

    private Instant failedAt;

    private String failureCode;

    private Instant createdAt;

    private Instant updatedAt;

    // --- Totalizações ---

    /**
     * Soma de itens PAYMENT (crédito)
     */
    private BigDecimal grossPayments;

    /**
     * Soma de itens REFUND (débito)
     */
    private BigDecimal grossRefunds;

    /**
     * Soma de itens PROVIDER_FEE (débito)
     */
    private BigDecimal providerFees;

    /**
     * Soma de itens CHARGEBACK (débito)
     */
    private BigDecimal chargebacks;

    /**
     * Soma de itens ADJUSTMENT (flexível)
     */
    private BigDecimal adjustments;

    /**
     * Saldo assinado calculado
     * = grossPayments - (grossRefunds + providerFees + chargebacks) + adjustments
     */
    private BigDecimal signedNetCalculated;

    /**
     * Saldo informado pelo gateway
     */
    private BigDecimal reportedNet;

    /**
     * Saldo já reconciliado com banco
     */
    private BigDecimal reconciled;

    /**
     * Saldo pendente de reconciliação
     * = reportedNet - reconciled
     */
    private BigDecimal outstanding;

    /**
     * Itens do repasse
     */
    private List<ProviderSettlementItemDTO> items;

    /**
     * DTO para item de repasse
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProviderSettlementItemDTO {

        private UUID id;

        private String itemType; // PAYMENT, REFUND, PROVIDER_FEE, CHARGEBACK, ADJUSTMENT

        private String direction; // CREDIT, DEBIT

        private BigDecimal amount;

        private UUID paymentId;

        private UUID refundId;

        private String externalReference;

        private String description;

        private Instant createdAt;
    }
}
