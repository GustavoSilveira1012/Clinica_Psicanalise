package com.psicogest.psicogest.repository;
import com.psicogest.psicogest.model.entity.MfaRecoveryCode;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {
    Optional<MfaRecoveryCode> findByUserIdAndCodeHashAndUsedAtIsNull(Long userId, String codeHash);
    @Modifying
    @Query("update MfaRecoveryCode c set c.usedAt = :now where c.user.id = :userId and c.usedAt is null")
    int invalidateForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
