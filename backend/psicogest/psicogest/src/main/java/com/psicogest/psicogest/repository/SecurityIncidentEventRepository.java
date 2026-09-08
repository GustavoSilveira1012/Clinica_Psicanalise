package com.psicogest.psicogest.repository;

import com.psicogest.psicogest.model.entity.SecurityIncidentEvent;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface SecurityIncidentEventRepository
        extends Repository<SecurityIncidentEvent, UUID> {

    SecurityIncidentEvent save(
            SecurityIncidentEvent event
    );

    List<SecurityIncidentEvent>
    findByIncidentIdOrderByOccurredAt(
            UUID incidentId
    );
}
