package com.psicogest.psicogest.model.entity.saas;

import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.enums.OrganizationInviteStatus;
import com.psicogest.psicogest.model.enums.OrganizationRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_invites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationInvite {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedBy;

    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;

    @Column(name = "email_ciphertext")
    private byte[] emailCiphertext;

    @Column(name = "email_iv")
    private byte[] emailIv;

    @Column(name = "email_encrypted_dek")
    private byte[] emailEncryptedDek;

    @Column(name = "email_key_id", length = 255)
    private String emailKeyId;

    @Column(name = "email_crypto_version")
    private Integer emailCryptoVersion;

    @Column(name = "email_crypto_algorithm", length = 30)
    private String emailCryptoAlgorithm;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrganizationRole role = OrganizationRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrganizationInviteStatus status = OrganizationInviteStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
