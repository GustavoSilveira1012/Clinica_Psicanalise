package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.AvailabilityExceptionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "availability_exceptions", indexes = {
                @Index(name = "idx_availability_exceptions_psychoanalyst_date", columnList = "psychoanalyst_id, exception_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityException {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "psychoanalyst_id", nullable = false)
        private Psychoanalyst psychoanalyst;

        @Column(name = "exception_date", nullable = false)
        private LocalDate date;

        @Enumerated(EnumType.STRING)
        @Column(name = "type", nullable = false, length = 30)
        private AvailabilityExceptionType type;

        @Column(name = "start_time")
        private LocalTime startTime;

        @Column(name = "end_time")
        private LocalTime endTime;

        @Column(length = 255)
        private String reason;

        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;

        @PrePersist
        protected void onCreate() {

                LocalDateTime now = LocalDateTime.now();

                createdAt = now;
                updatedAt = now;
        }

        @PreUpdate
        protected void onUpdate() {
                updatedAt = LocalDateTime.now();
        }
}
