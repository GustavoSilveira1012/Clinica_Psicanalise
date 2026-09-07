package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.RefreshToken;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository
        extends JpaRepository<
                RefreshToken,
                UUID
        > {

    @Lock(
            LockModeType.PESSIMISTIC_WRITE
    )
    @Query("""
            SELECT r
            FROM RefreshToken r
            JOIN FETCH r.user
            WHERE r.tokenHash = :hash
            """)
    Optional<RefreshToken>
    findByTokenHashForUpdate(
            @Param("hash")
            String hash
    );

    @Modifying
    @Query("""
            UPDATE RefreshToken r

            SET r.revokedAt = :now,
                r.revocationReason = :reason

            WHERE r.familyId = :familyId
              AND r.revokedAt IS NULL
            """)
    int revokeFamily(
            @Param("familyId")
            UUID familyId,

            @Param("now")
            LocalDateTime now,

            @Param("reason")
            String reason
    );

    @Modifying
    @Query("""
            UPDATE RefreshToken r

            SET r.revokedAt = :now,
                r.revocationReason = :reason

            WHERE r.user.id = :userId
              AND r.revokedAt IS NULL
            """)
    int revokeAllForUser(
            @Param("userId")
            Long userId,

            @Param("now")
            LocalDateTime now,

            @Param("reason")
            String reason
    );
}