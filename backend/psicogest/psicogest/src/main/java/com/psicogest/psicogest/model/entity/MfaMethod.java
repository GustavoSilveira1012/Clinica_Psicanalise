package com.psicogest.psicogest.model.entity;

import com.psicogest.psicogest.model.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mfa_methods")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MfaMethod {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private MfaMethodType methodType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private MfaMethodStatus status;
    @Column(length = 100) private String label;
    @Column(columnDefinition = "text") private String secretCiphertext;
    @Column(length = 100) private String secretIv;
    private UUID enrollmentChallengeId;
    private Long lastAcceptedTimeStep;
    @Column(nullable = false) private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;
}
