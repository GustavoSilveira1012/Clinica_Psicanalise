package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.SecurityAlert;
import com.psicogest.psicogest.model.enums.SecurityAlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityAlertRepository
        extends JpaRepository<SecurityAlert, UUID> {

    List<SecurityAlert>
    findByStatusOrderByDetectedAtDesc(
            SecurityAlertStatus status
    );

    List<SecurityAlert>
    findByStatusInAndDetectedAtAfterOrderByDetectedAtDesc(
            List<SecurityAlertStatus> statuses,
            Instant after
    );
}
