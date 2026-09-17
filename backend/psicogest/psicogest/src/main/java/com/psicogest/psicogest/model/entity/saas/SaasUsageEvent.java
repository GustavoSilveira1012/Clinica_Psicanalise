package com.psicogest.psicogest.model.entity.saas;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saas_usage_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasUsageEvent {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "feature_code", nullable = false, length = 80)
    private String featureCode;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
