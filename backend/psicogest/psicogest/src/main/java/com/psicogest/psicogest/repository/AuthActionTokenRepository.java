package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.AuthActionToken;
import com.psicogest.psicogest.model.enums.AuthActionTokenType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AuthActionTokenRepository extends JpaRepository<AuthActionToken, UUID> {

    @Query("SELECT t.user.id FROM AuthActionToken t WHERE t.tokenHash = :hash")
    Optional<Long> findUserIdByTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("""
            UPDATE AuthActionToken t
            SET t.consumedAt = :now
            WHERE t.user.id = :userId
              AND t.tokenType = :type
              AND t.consumedAt IS NULL
            """)
    int invalidatePending(
            @Param("userId") Long userId,
            @Param("type") AuthActionTokenType type,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM AuthActionToken t JOIN FETCH t.user WHERE t.tokenHash = :hash")
    Optional<AuthActionToken> findByTokenHashForUpdate(@Param("hash") String hash);
}
