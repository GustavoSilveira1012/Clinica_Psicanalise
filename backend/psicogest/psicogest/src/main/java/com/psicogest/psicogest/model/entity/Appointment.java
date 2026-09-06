package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.AppointmentStatus;
import com.psicogest.psicogest.model.enums.AppointmentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments", indexes = {
                @Index(name = "idx_appointments_psychoanalyst_period", columnList = "psychoanalyst_id, scheduled_start, scheduled_end"),
                @Index(name = "idx_appointments_patient", columnList = "patient_id"),
                @Index(name = "idx_appointments_recurring_group", columnList = "recurring_group_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "patient_id", nullable = false)
        private Patient patient;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "psychoanalyst_id", nullable = false)
        private Psychoanalyst psychoanalyst;

        /*
         * Contexto administrativo.
         *
         * NÃO significa que a clínica seja dona
         * do atendimento ou prontuário.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "clinic_membership_id")
        private ClinicMembership clinicMembership;

        /*
         * Usado para preservar histórico
         * em um reagendamento.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "original_appointment_id")
        private Appointment originalAppointment;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "appointment_series_id")
        private AppointmentSeries appointmentSeries;

        @Column(name = "occurrence_number")
        private Integer occurrenceNumber;

        @Column(name = "recurring_group_id")
        private UUID recurringGroupId;

        @Column(name = "scheduled_start", nullable = false)
        private LocalDateTime scheduledStart;

        @Column(name = "scheduled_end", nullable = false)
        private LocalDateTime scheduledEnd;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private AppointmentStatus status;

        @Enumerated(EnumType.STRING)
        @Column(name = "appointment_type", nullable = false, length = 30)
        private AppointmentType appointmentType;

        @Column(name = "cancellation_reason", length = 255)
        private String cancellationReason;

        @Column(name = "cancelled_at")
        private LocalDateTime cancelledAt;

        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;

        @Column(name = "confirmed_at")
        private LocalDateTime confirmedAt;

        @Column(name = "completed_at")
        private LocalDateTime completedAt;

        @Column(name = "no_show_at")
        private LocalDateTime noShowAt;

        @Column(name = "rescheduled_at")
        private LocalDateTime rescheduledAt;

        @PrePersist
        protected void onCreate() {

                LocalDateTime now = LocalDateTime.now();

                createdAt = now;
                updatedAt = now;

                if (status == null) {
                        status = AppointmentStatus.SCHEDULED;
                }
        }

        @PreUpdate
        protected void onUpdate() {
                updatedAt = LocalDateTime.now();
        }
}