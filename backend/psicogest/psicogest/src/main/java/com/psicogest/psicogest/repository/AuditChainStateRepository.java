package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.AuditChainState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AuditChainStateRepository
        extends JpaRepository<
                AuditChainState,
                Short
        > {

    @Lock(
            LockModeType.PESSIMISTIC_WRITE
    )
    @Query("""
        SELECT s
        FROM AuditChainState s
        WHERE s.id = 1
        """)
    AuditChainState lockChain();
}