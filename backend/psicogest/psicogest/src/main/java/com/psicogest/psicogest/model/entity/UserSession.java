package com.psicogest.psicogest.model.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "user_sessions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserSession {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime lastSeenAt;
    @Column(nullable = false) private LocalDateTime expiresAt;
    @Column(length = 45) private String createdIp;
    @Column(length = 45) private String lastIp;
    @Column(length = 64) private String userAgentHash;
    @Column(length = 100) private String deviceLabel;
    private LocalDateTime revokedAt;
    @Column(length = 100) private String revocationReason;
}
