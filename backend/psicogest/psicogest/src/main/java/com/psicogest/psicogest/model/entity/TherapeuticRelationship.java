package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.TherapeuticRelationshipStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "therapeutic_relationships", indexes = {
                @Index(name = "idx_therapeutic_relationship_patient", columnList = "patient_id"),
                @Index(name = "idx_therapeutic_relationship_psychoanalyst", columnList = "psychoanalyst_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TherapeuticRelationship {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "patient_id", nullable = false)
        private Patient patient;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "psychoanalyst_id", nullable = false)
        private Psychoanalyst psychoanalyst;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private TherapeuticRelationshipStatus status;

        @Column(name = "is_primary", nullable = false)
        private Boolean primary;

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
                        status = TherapeuticRelationshipStatus.ACTIVE;
                }

                if (primary == null) {
                        primary = false;
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
