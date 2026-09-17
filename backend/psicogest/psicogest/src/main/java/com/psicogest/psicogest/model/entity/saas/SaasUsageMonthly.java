package com.psicogest.psicogest.model.entity.saas;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "saas_usage_monthly")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasUsageMonthly {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "feature_code", nullable = false, length = 80)
    private String featureCode;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    @Builder.Default
    private Long quantity = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onWrite() {
        if (id == null) id = UUID.randomUUID();
        updatedAt = Instant.now();
    }
}
