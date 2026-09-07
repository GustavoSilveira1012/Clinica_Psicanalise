package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.AuthenticationChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AuthenticationChallengeRepository extends JpaRepository<AuthenticationChallenge, UUID> {
    @Query("select c.user.id from AuthenticationChallenge c where c.tokenHash = :hash")
    Optional<Long> findUserIdByHash(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AuthenticationChallenge c join fetch c.user where c.tokenHash = :hash")
    Optional<AuthenticationChallenge> findByTokenHashForUpdate(@Param("hash") String hash);
}
