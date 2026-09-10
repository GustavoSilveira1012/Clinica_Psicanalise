package com.psicogest.psicogest.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

import com.psicogest.psicogest.model.enums.PaymentProviderType;
import com.psicogest.psicogest.model.enums.ProviderSettlementDirection;
import com.psicogest.psicogest.model.enums.ProviderSettlementStatus;
import com.psicogest.psicogest.model.enums.ProviderSettlementValidationStatus;

/**
 * Repasse de gateway
 * 
 * Representa liquidação de:
 * - pagamentos
 * - refunds
 * - taxas
 * - chargebacks
 * 
 * Gateway → ProviderSettlement → BankTransaction
 */
@Entity
@Table(name = "provider_settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderSettlement {

    /**
     * ID único
     */
    @Id
    private UUID id;

    /**
     * Clínica proprietária
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "clinic_id",
            nullable = false,
            updatable = false
    )
    private Clinic clinic;

    /**
     * Gateway (Mercado Pago, Stripe, etc)
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            updatable = false
    )
    private PaymentProviderType provider;

    /**
     * ID do repasse no gateway
     */
    @Column(
            name = "provider_settlement_id",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String providerSettlementId;

    /**
     * Status do ciclo de vida
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderSettlementStatus status;

    /**
     * Status da validação
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false)
    private ProviderSettlementValidationStatus validationStatus;

    /**
     * Direção do repasse
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_direction")
    private ProviderSettlementDirection settlementDirection;

    /**
     * Valor líquido informado pelo gateway
     */
    @Column(
            name = "reported_net_amount",
            precision = 19,
            scale = 2
    )
    private BigDecimal reportedNetAmount;

    /**
     * Moeda
     */
    @Column(
            nullable = false,
            length = 3,
            updatable = false
    )
    private String currency;

    /**
     * Data esperada do repasse
     */
    private Instant expectedAt;

    /**
     * Data do repasse (confirmação do gateway)
     */
    private Instant settledAt;

    /**
     * Data da validação
     */
    private Instant validatedAt;

    /**
     * Data do erro (se falhou)
     */
    private Instant failedAt;

    /**
     * Código do erro (se falhou)
     */
    @Column(length = 100)
    private String failureCode;

    /**
     * Timestamps
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Versionamento
     */
    @Version
    private Long version;

    /**
     * Marca como validado
     */
    public void markValidated(Instant now) {

        if (status != ProviderSettlementStatus.RECEIVED) {

            throw new IllegalStateException(
                    "Repasse não pode ser validado neste status"
            );
        }

        validationStatus =
                ProviderSettlementValidationStatus.MATCHED;

        status = ProviderSettlementStatus.VALIDATED;

        validatedAt = now;

        updatedAt = now;
    }

    /**
     * Marca como divergência
     */
    public void markMismatch(Instant now) {

        validationStatus =
                ProviderSettlementValidationStatus.MISMATCHED;

        validatedAt = now;

        updatedAt = now;
    }

    /**
     * Marca como liquidado
     */
    public void markSettled(Instant now) {

        if (status != ProviderSettlementStatus.VALIDATED) {

            throw new IllegalStateException(
                    "Somente repasse validado pode ser liquidado"
            );
        }

        if (validationStatus !=
                ProviderSettlementValidationStatus.MATCHED) {

            throw new IllegalStateException(
                    "Repasse com divergência não pode ser liquidado"
            );
        }

        status = ProviderSettlementStatus.SETTLED;

        settledAt = now;

        updatedAt = now;
    }

    /**
     * Marca como parcialmente reconciliado
     */
    public void markPartiallyReconciled(Instant now) {

        updatedAt = now;
    }

    /**
     * Marca como totalmente reconciliado
     */
    public void markReconciled(Instant now) {

        updatedAt = now;
    }
}
