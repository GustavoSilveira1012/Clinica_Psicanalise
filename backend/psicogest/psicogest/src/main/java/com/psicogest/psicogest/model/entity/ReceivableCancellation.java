package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.ReceivableCancellationMode;
import com.psicogest.psicogest.model.enums.ReceivableCancellationReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 8. Registro de cancelamento de cobrança
 * 
 * Rastreia o cancelamento com modo de liquidação
 * Estados: PENDING → COMPLETED ou FAILED
 */
@Entity
@Table(name = "receivable_cancellations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivableCancellation {

    @Id
    private UUID id;

    /**
     * Cobrança cancelada
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "receivable_id",
            nullable = false,
            updatable = false
    )
    private Receivable receivable;

    /**
     * Motivo do cancelamento
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceivableCancellationReason reason;

    /**
     * Modo de liquidação: NONE, REFUND, CREDIT_BALANCE
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceivableCancellationMode mode;

    /**
     * Status do cancelamento
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceivableCancellationStatus status;

    /**
     * Quem solicitou
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by_user_id",
            nullable = false,
            updatable = false
    )
    private User createdBy;

    /**
     * Quando foi solicitado
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    /**
     * Quando foi concluído
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Quando falhou
     */
    @Column(name = "failed_at")
    private Instant failedAt;

    /**
     * Quando foi atualizado
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Versionamento (OCC)
     */
    @Version
    private Long version;

    /**
     * Status do cancelamento
     */
    public enum ReceivableCancellationStatus {
        PENDING,      // Aguardando conclusão (refunds pendentes)
        COMPLETED,    // Cancelamento concluído
        FAILED        // Cancelamento falhou (alguns refunds falharam)
    }
}
