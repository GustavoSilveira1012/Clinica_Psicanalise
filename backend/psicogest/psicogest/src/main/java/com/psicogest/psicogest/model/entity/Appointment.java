package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.AppointmentStatus;
import com.psicogest.psicogest.model.enums.AppointmentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", indexes = {
                @Index(name = "idx_appointments_psychoanalyst_period", columnList = "psychoanalyst_id, scheduled_start, scheduled_end"),
                @Index(name = "idx_appointments_patient", columnList = "patient_id")
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

        // Lombok-generated methods (explicitly added due to annotation processor issue)
        public Long getId() {
                return id;
        }

        public LocalDateTime getScheduledStart() {
                return scheduledStart;
        }

        public LocalDateTime getScheduledEnd() {
                return scheduledEnd;
        }

        public LocalDateTime getCompletedAt() {
                return completedAt;
        }

        public void setCompletedAt(LocalDateTime completedAt) {
                this.completedAt = completedAt;
        }

        public LocalDateTime getNoShowAt() {
                return noShowAt;
        }

        public void setNoShowAt(LocalDateTime noShowAt) {
                this.noShowAt = noShowAt;
        }

        public LocalDateTime getRescheduledAt() {
                return rescheduledAt;
        }

        public void setRescheduledAt(LocalDateTime rescheduledAt) {
                this.rescheduledAt = rescheduledAt;
        }

        public LocalDateTime getCancelledAt() {
                return cancelledAt;
        }

        public void setCancelledAt(LocalDateTime cancelledAt) {
                this.cancelledAt = cancelledAt;
        }

        public void setCancellationReason(String cancellationReason) {
                this.cancellationReason = cancellationReason;
        }

        public Patient getPatient() {
                return patient;
        }

        public Psychoanalyst getPsychoanalyst() {
                return psychoanalyst;
        }

        public ClinicMembership getClinicMembership() {
                return clinicMembership;
        }

        public Appointment getOriginalAppointment() {
                return originalAppointment;
        }

        public AppointmentSeries getAppointmentSeries() {
                return appointmentSeries;
        }

        public Integer getOccurrenceNumber() {
                return occurrenceNumber;
        }

        public AppointmentType getAppointmentType() {
                return appointmentType;
        }

        public AppointmentStatus getStatus() {
                return status;
        }

        public void setStatus(AppointmentStatus status) {
                this.status = status;
        }

        public String getCancellationReason() {
                return cancellationReason;
        }

        public LocalDateTime getConfirmedAt() {
                return confirmedAt;
        }

        public void setConfirmedAt(LocalDateTime confirmedAt) {
                this.confirmedAt = confirmedAt;
        }

        // Builder pattern support
        public static AppointmentBuilder builder() {
                return new AppointmentBuilder();
        }

        public static class AppointmentBuilder {
                private Long id;
                private Patient patient;
                private Psychoanalyst psychoanalyst;
                private ClinicMembership clinicMembership;
                private Appointment originalAppointment;
                private AppointmentSeries appointmentSeries;
                private Integer occurrenceNumber;
                private LocalDateTime scheduledStart;
                private LocalDateTime scheduledEnd;
                private AppointmentStatus status;
                private AppointmentType appointmentType;
                private String cancellationReason;
                private LocalDateTime cancelledAt;
                private LocalDateTime createdAt;
                private LocalDateTime updatedAt;
                private LocalDateTime confirmedAt;
                private LocalDateTime completedAt;
                private LocalDateTime noShowAt;
                private LocalDateTime rescheduledAt;

                public AppointmentBuilder id(Long id) {
                        this.id = id;
                        return this;
                }

                public AppointmentBuilder patient(Patient patient) {
                        this.patient = patient;
                        return this;
                }

                public AppointmentBuilder psychoanalyst(Psychoanalyst psychoanalyst) {
                        this.psychoanalyst = psychoanalyst;
                        return this;
                }

                public AppointmentBuilder clinicMembership(ClinicMembership clinicMembership) {
                        this.clinicMembership = clinicMembership;
                        return this;
                }

                public AppointmentBuilder originalAppointment(Appointment originalAppointment) {
                        this.originalAppointment = originalAppointment;
                        return this;
                }

                public AppointmentBuilder appointmentSeries(AppointmentSeries appointmentSeries) {
                        this.appointmentSeries = appointmentSeries;
                        return this;
                }

                public AppointmentBuilder occurrenceNumber(Integer occurrenceNumber) {
                        this.occurrenceNumber = occurrenceNumber;
                        return this;
                }

                public AppointmentBuilder scheduledStart(LocalDateTime scheduledStart) {
                        this.scheduledStart = scheduledStart;
                        return this;
                }

                public AppointmentBuilder scheduledEnd(LocalDateTime scheduledEnd) {
                        this.scheduledEnd = scheduledEnd;
                        return this;
                }

                public AppointmentBuilder status(AppointmentStatus status) {
                        this.status = status;
                        return this;
                }

                public AppointmentBuilder appointmentType(AppointmentType appointmentType) {
                        this.appointmentType = appointmentType;
                        return this;
                }

                public AppointmentBuilder cancellationReason(String cancellationReason) {
                        this.cancellationReason = cancellationReason;
                        return this;
                }

                public AppointmentBuilder cancelledAt(LocalDateTime cancelledAt) {
                        this.cancelledAt = cancelledAt;
                        return this;
                }

                public AppointmentBuilder createdAt(LocalDateTime createdAt) {
                        this.createdAt = createdAt;
                        return this;
                }

                public AppointmentBuilder updatedAt(LocalDateTime updatedAt) {
                        this.updatedAt = updatedAt;
                        return this;
                }

                public AppointmentBuilder confirmedAt(LocalDateTime confirmedAt) {
                        this.confirmedAt = confirmedAt;
                        return this;
                }

                public AppointmentBuilder completedAt(LocalDateTime completedAt) {
                        this.completedAt = completedAt;
                        return this;
                }

                public AppointmentBuilder noShowAt(LocalDateTime noShowAt) {
                        this.noShowAt = noShowAt;
                        return this;
                }

                public AppointmentBuilder rescheduledAt(LocalDateTime rescheduledAt) {
                        this.rescheduledAt = rescheduledAt;
                        return this;
                }

                public Appointment build() {
                        Appointment appointment = new Appointment();
                        appointment.id = this.id;
                        appointment.patient = this.patient;
                        appointment.psychoanalyst = this.psychoanalyst;
                        appointment.clinicMembership = this.clinicMembership;
                        appointment.originalAppointment = this.originalAppointment;
                        appointment.appointmentSeries = this.appointmentSeries;
                        appointment.occurrenceNumber = this.occurrenceNumber;
                        appointment.scheduledStart = this.scheduledStart;
                        appointment.scheduledEnd = this.scheduledEnd;
                        appointment.status = this.status;
                        appointment.appointmentType = this.appointmentType;
                        appointment.cancellationReason = this.cancellationReason;
                        appointment.cancelledAt = this.cancelledAt;
                        appointment.createdAt = this.createdAt;
                        appointment.updatedAt = this.updatedAt;
                        appointment.confirmedAt = this.confirmedAt;
                        appointment.completedAt = this.completedAt;
                        appointment.noShowAt = this.noShowAt;
                        appointment.rescheduledAt = this.rescheduledAt;
                        return appointment;
                }
        }
}