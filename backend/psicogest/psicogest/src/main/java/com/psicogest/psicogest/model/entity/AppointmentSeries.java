package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.AppointmentSeriesStatus;
import com.psicogest.psicogest.model.enums.RecurrenceFrequency;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "appointment_series", indexes = {
        @Index(name = "idx_appointment_series_patient", columnList = "patient_id"),
        @Index(name = "idx_appointment_series_psychoanalyst", columnList = "psychoanalyst_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentSeries {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "psychoanalyst_id", nullable = false)
    private Psychoanalyst psychoanalyst;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_membership_id")
    private ClinicMembership clinicMembership;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecurrenceFrequency frequency;

    @Column(name = "recurrence_interval", nullable = false)
    private Integer recurrenceInterval;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on")
    private LocalDate endsOn;

    @Column(name = "total_occurrences")
    private Integer totalOccurrences;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppointmentSeriesStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (status == null) {
            status = AppointmentSeriesStatus.ACTIVE;
        }

        if (recurrenceInterval == null) {
            recurrenceInterval = 1;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}