package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.SecurityEvent;
import com.psicogest.psicogest.repository.SecurityEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SecurityEventService {

    private final SecurityEventRepository repository;

    public SecurityEventService(
            SecurityEventRepository repository
    ) {
        this.repository = repository;
    }

    public void record(SecurityEvent event) {
        repository.save(event);
    }
}
