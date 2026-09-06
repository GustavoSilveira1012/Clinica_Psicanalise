package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.ClinicMembershipPeriodStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clinic_membership_periods", indexes = {
                @Index(name = "idx_clinic_membership_period_membership", columnList = "clinic_membership_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicMembershipPeriod {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "clinic_membership_id", nullable = false)
        private ClinicMembership membership;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private ClinicMembershipPeriodStatus status;

        @Column(name = "started_at", nullable = false)
        private LocalDateTime startedAt;

        @Column(name = "ended_at")
        private LocalDateTime endedAt;

        @Column(name = "end_reason", length = 255)
        private String endReason;

        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;

        @PrePersist
        protected void onCreate() {

                LocalDateTime now = LocalDateTime.now();

                if (status == null) {
                        status = ClinicMembershipPeriodStatus.ACTIVE;
                }

                if (startedAt == null) {
                        startedAt = now;
                }

                createdAt = now;
                updatedAt = now;
        }

        @PreUpdate
        protected void onUpdate() {

                updatedAt = LocalDateTime.now();
        }
}