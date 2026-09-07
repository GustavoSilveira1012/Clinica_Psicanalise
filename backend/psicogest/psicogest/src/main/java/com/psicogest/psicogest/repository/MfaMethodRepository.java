package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.MfaMethod;
import com.psicogest.psicogest.model.enums.MfaMethodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface MfaMethodRepository extends JpaRepository<MfaMethod, UUID> {
    boolean existsByUserIdAndStatus(Long userId, MfaMethodStatus status);
    Optional<MfaMethod> findByUserIdAndStatus(Long userId, MfaMethodStatus status);
}
