package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "clinic_memberships", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clinic_membership_user", columnNames = { "clinic_id", "psychoanalyst_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "psychoanalyst_id", nullable = false)
    private Psychoanalyst psychoanalyst;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private MembershipStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {

        if (status == null) {
            status = MembershipStatus.ACTIVE;
        }

        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }

    }

}
