package com.psicogest.psicogest.model.entity.saas;

import com.psicogest.psicogest.model.enums.OnboardingStatus;
import com.psicogest.psicogest.model.enums.OnboardingStep;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "organization_onboarding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationOnboarding {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 40)
    @Builder.Default
    private OnboardingStep currentStep = OnboardingStep.PROFILE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OnboardingStatus status = OnboardingStatus.IN_PROGRESS;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "completed_steps", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<String> completedSteps = List.of();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
