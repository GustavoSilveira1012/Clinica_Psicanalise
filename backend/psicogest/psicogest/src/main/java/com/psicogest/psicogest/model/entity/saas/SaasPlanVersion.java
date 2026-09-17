package com.psicogest.psicogest.model.entity.saas;

import com.psicogest.psicogest.model.enums.SaasPlanVersionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saas_plan_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasPlanVersion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saas_plan_id", nullable = false)
    private SaasPlan plan;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaasPlanVersionStatus status;

    @Column(name = "monthly_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "BRL";

    @Column(name = "trial_days", nullable = false)
    @Builder.Default
    private Integer trialDays = 14;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
