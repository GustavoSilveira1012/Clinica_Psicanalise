package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.AuthenticationChallengeType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "authentication_challenges")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthenticationChallenge {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40) private AuthenticationChallengeType challengeType;
    @Column(nullable = false) private Integer securityVersion;
    @Column(nullable = false) private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    @Column(nullable = false) private int attemptCount;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(length = 45) private String createdIp;
    @Column(length = 64) private String userAgentHash;
}
