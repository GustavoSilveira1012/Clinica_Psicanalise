package com.psicogest.psicogest.model.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "mfa_recovery_codes")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MfaRecoveryCode {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, unique = true, length = 64) private String codeHash;
    @Column(nullable = false) private LocalDateTime createdAt;
    private LocalDateTime usedAt;
}
