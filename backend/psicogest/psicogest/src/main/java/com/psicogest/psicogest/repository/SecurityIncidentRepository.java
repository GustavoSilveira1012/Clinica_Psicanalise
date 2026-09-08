package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.SecurityIncident;
import com.psicogest.psicogest.model.enums.SecurityIncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityIncidentRepository
        extends JpaRepository<SecurityIncident, UUID> {

    List<SecurityIncident>
    findByStatusInOrderByDetectedAtDesc(
            Collection<SecurityIncidentStatus> statuses
    );

    boolean existsBySourceAlertId(
            UUID sourceAlertId
    );
}
