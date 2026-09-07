package com.psicogest.psicogest.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_tokens"
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            name = "family_id",
            nullable = false
    )
    private UUID familyId;

    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String tokenHash;

    @Column(
            name = "security_version",
            nullable = false
    )
    private Integer securityVersion;

    @Column(
            name = "issued_at",
            nullable = false
    )
    private LocalDateTime issuedAt;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(
            name = "revocation_reason",
            length = 100
    )
    private String revocationReason;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "replaced_by_id"
    )
    private RefreshToken replacedBy;

    @Column(
            name = "created_ip",
            length = 45
    )
    private String createdIp;

    @Column(
            name = "user_agent_hash",
            length = 64
    )
    private String userAgentHash;
}