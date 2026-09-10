package com.psicogest.psicogest.domain.finance;

import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * 31. Helper para auditoria de eventos financeiros
 * 
 * Encapsula criação de AuditCommand com metadados financeiros seguros
 * 
 * Segurança: NUNCA audita dados sensíveis
 * ✅ amount, currency, paymentMethod
 * ❌ Dados completos de cartão
 * ❌ CVV
 * ❌ Token de pagamento
 * ❌ Chave privada/provider secret
 */
public final class FinanceAuditHelper {

    private FinanceAuditHelper() {
        // Utility class
    }

    /**
     * Cria comando para auditoria de receivable criado
     */
    public static AuditCommand receivableCreated(
            Long actorUserId,
            Long patientId,
            Long clinicId,
            UUID receivableId,
            BigDecimal netAmount,
            String currency
    ) {

        return new AuditCommand(
                actorUserId,
                null,
                AuditAction.RECEIVABLE_CREATED,
                "RECEIVABLE",
                receivableId.toString(),
                patientId,
                clinicId,
                AuditOutcome.SUCCESS,
                null,
                null,
                null,
                Map.of(
                        "amount", netAmount.toPlainString(),
                        "currency", currency
                )
        );
    }

    /**
     * Cria comando para auditoria de receivable cancelado
     */
    public static AuditCommand receivableCancelled(
            Long actorUserId,
            Long patientId,
            Long clinicId,
            UUID receivableId,
            String cancellationReason
    ) {

        return new AuditCommand(
                actorUserId,
                null,
                AuditAction.RECEIVABLE_CANCELLED,
                "RECEIVABLE",
                receivableId.toString(),
                patientId,
                clinicId,
                AuditOutcome.SUCCESS,
                null,
                null,
                null,
                Map.of(
                        "reason", cancellationReason != null
                                ? cancellationReason
                                : "Não informado"
                )
        );
    }

    /**
     * Cria comando para auditoria de pagamento criado
     */
    public static AuditCommand paymentCreated(
            Long actorUserId,
            Long patientId,
            Long clinicId,
            UUID paymentId,
            BigDecimal amount,
            String currency,
            String paymentMethod
    ) {

        return new AuditCommand(
                actorUserId,
                null,
                AuditAction.PAYMENT_CREATED,
                "PAYMENT",
                paymentId.toString(),
                patientId,
                clinicId,
                AuditOutcome.SUCCESS,
                null,
                null,
                null,
                Map.of(
                        "amount", amount.toPlainString(),
                        "currency", currency,
                        "paymentMethod", paymentMethod
                )
        );
    }

    /**
     * Cria comando para auditoria de pagamento confirmado
     */
    public static AuditCommand paymentConfirmed(
            Long actorUserId,
            Long patientId,
            Long clinicId,
            UUID paymentId,
            BigDecimal amount,
            String currency
    ) {

        return new AuditCommand(
                actorUserId,
                null,
                AuditAction.PAYMENT_CONFIRMED,
                "PAYMENT",
                paymentId.toString(),
                patientId,
                clinicId,
                AuditOutcome.SUCCESS,
                null,
                null,
                null,
                Map.of(
                        "amount", amount.toPlainString(),
                        "currency", currency
                )
        );
    }

    /**
     * Cria comando para auditoria de pagamento falhado
     */
    public static AuditCommand paymentFailed(
            Long actorUserId,
            Long patientId,
            Long clinicId,
            UUID paymentId,
            BigDecimal amount,
            String currency,
            String failureReason
    ) {

        return new AuditCommand(
                actorUserId,
                null,
                AuditAction.PAYMENT_FAILED,
                "PAYMENT",
                paymentId.toString(),
                patientId,
                clinicId,
                AuditOutcome.FAILURE,
                null,
                null,
                null,
                Map.of(
                        "amount", amount.toPlainString(),
                        "currency", currency,
                        "reason", failureReason != null
                                ? failureReason
                                : "Falha desconhecida"
                )
        );
    }

    /**
     * Cria comando para auditoria de pagamento cancelado
     */
    public static AuditCommand paymentCancelled(
            Long actorUserId,
            Long patientId,
            Long clinicId,
            UUID paymentId,
            BigDecimal amount,
            String currency
    ) {

        return new AuditCommand(
                actorUserId,
                null,
                AuditAction.PAYMENT_CANCELLED,
                "PAYMENT",
                paymentId.toString(),
                patientId,
                clinicId,
                AuditOutcome.SUCCESS,
                null,
                null,
                null,
                Map.of(
                        "amount", amount.toPlainString(),
                        "currency", currency
                )
        );
    }

    /**
     * Cria comando para auditoria de alocação de pagamento
     */
    public static AuditCommand paymentAllocated(
            Long actorUserId,
            Long patientId,
            Long clinicId,
            UUID paymentId,
            UUID receivableId,
            BigDecimal allocationAmount,
            String currency
    ) {

        return new AuditCommand(
                actorUserId,
                null,
                AuditAction.PAYMENT_ALLOCATED,
                "PAYMENT_ALLOCATION",
                String.format(
                        "%s->%s",
                        paymentId,
                        receivableId
                ),
                patientId,
                clinicId,
                AuditOutcome.SUCCESS,
                null,
                null,
                null,
                Map.of(
                        "paymentId", paymentId.toString(),
                        "receivableId", receivableId.toString(),
                        "amount", allocationAmount.toPlainString(),
                        "currency", currency
                )
        );
    }

    /**
     * Cria comando para auditoria de reembolso solicitado
     */
    public static AuditCommand refundRequested(
            Long actorUserId,
            Long patientId,
            Long clinicId,
            UUID paymentId,
            BigDecimal refundAmount,
            String currency,
            String reason
    ) {

        return new AuditCommand(
                actorUserId,
                null,
                AuditAction.REFUND_REQUESTED,
                "REFUND",
                paymentId.toString(),
                patientId,
                clinicId,
                AuditOutcome.SUCCESS,
                null,
                null,
                null,
                Map.of(
                        "amount", refundAmount.toPlainString(),
                        "currency", currency,
                        "reason", reason != null ? reason : "Não informado"
                )
        );
    }

    /**
     * Cria comando para auditoria de reembolso confirmado
     */
    public static AuditCommand refundConfirmed(
            Long actorUserId,
            Long patientId,
            Long clinicId,
            UUID paymentId,
            BigDecimal refundAmount,
            String currency
    ) {

        return new AuditCommand(
                actorUserId,
                null,
                AuditAction.REFUND_CONFIRMED,
                "REFUND",
                paymentId.toString(),
                patientId,
                clinicId,
                AuditOutcome.SUCCESS,
                null,
                null,
                null,
                Map.of(
                        "amount", refundAmount.toPlainString(),
                        "currency", currency
                )
        );
    }
}
