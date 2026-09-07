package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.ClinicAccessRole;
import com.psicogest.psicogest.model.enums.ClinicUserMembershipStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clinic_user_memberships")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicUserMembership {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_role")
    private ClinicAccessRole accessRole;

    @Enumerated(EnumType.STRING)
    private ClinicUserMembershipStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "end_reason")
    private String endReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {

        LocalDateTime now =
                LocalDateTime.now();

        if (status == null) {
            status =
                    ClinicUserMembershipStatus.ACTIVE;
        }

        if (accessRole == null) {
            accessRole =
                    ClinicAccessRole.ADMIN;
        }

        if (startedAt == null) {
            startedAt = now;
        }

        createdAt = now;
    }
}