package com.psicogest.psicogest.repository;
import com.psicogest.psicogest.model.entity.UserSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByIdAndUserId(UUID id, Long userId);
    List<UserSession> findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastSeenAtDesc(Long userId, LocalDateTime now);
    boolean existsByIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID id, Long userId, LocalDateTime now);
    @Modifying
    @Query("update UserSession s set s.revokedAt = :now, s.revocationReason = :reason where s.user.id = :userId and s.revokedAt is null")
    int revokeAll(@Param("userId") Long userId, @Param("now") LocalDateTime now, @Param("reason") String reason);
}
