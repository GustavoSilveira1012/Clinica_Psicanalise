package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(u.email) = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u
            FROM User u
            WHERE u.id = :userId
            """)
    Optional<User> findByIdForSecurityUpdate1(@Param("userId") Long userId);

    interface UserSecurityView {
        Boolean getActive();
        Integer getSecurityVersion();
        LocalDateTime getLockedUntil();
    }

    @Query("""
            SELECT u.active AS active,
                   u.securityVersion AS securityVersion,
                   u.lockedUntil AS lockedUntil
            FROM User u
            WHERE u.id = :id
            """)
    Optional<UserSecurityView> findProjectedById(@Param("id") Long id);

    Optional<User> findByIdForSecurityUpdate(Long userId);
}
