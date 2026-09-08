package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.AppointmentSeriesStatus;
import com.psicogest.psicogest.model.enums.RecurrenceFrequency;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_chain_state")
@Getter
@Setter
@NoArgsConstructor
public class AuditChainState {

    @Id
    private Short id;

    @Column(name = "last_sequence")
    private Long lastSequence;

    @Column(name = "last_mac")
    private String lastMac;

    @Column(name = "updated_at")
    private Instant updatedAt;
}