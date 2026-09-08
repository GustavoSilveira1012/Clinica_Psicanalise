package com.psicogest.psicogest.security.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.model.entity.SecurityEvent;

@Service
public class SecurityEventService {

    private final com.psicogest.psicogest.repository.SecurityEventRepository repository;

    public SecurityEventService(
            com.psicogest.psicogest.repository.SecurityEventRepository repository
    ) {

        this.repository = repository;
    }

    @Transactional(
            propagation =
                    Propagation.REQUIRES_NEW
    )
    public void record(
            SecurityEvent event
    ) {

        repository.save(event);
    }
}