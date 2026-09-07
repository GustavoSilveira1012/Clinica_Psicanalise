package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

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
    Optional<UserSecurityView> findProjectedById(
            @Param("id") Long id
    );
}
