package com.psicogest.psicogest.security.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.model.entity.SecurityEvent;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;
import com.psicogest.psicogest.security.tenant.TenantDatabaseContext;

@Service
public class SecurityEventService {

    private final com.psicogest.psicogest.repository.SecurityEventRepository repository;
    private final TenantDatabaseContext tenantDatabaseContext;

    public SecurityEventService(
            com.psicogest.psicogest.repository.SecurityEventRepository repository,
            TenantDatabaseContext tenantDatabaseContext
    ) {

        this.repository = repository;
        this.tenantDatabaseContext = tenantDatabaseContext;
    }

    @Transactional(
            propagation =
                    Propagation.REQUIRES_NEW
    )
    public void record(
            SecurityEvent event
    ) {
        TenantContext context = TenantContextHolder.get();
        if (context != null) {
            tenantDatabaseContext.applyUser(context.userId());
            if (event.getOrganizationId() == null) {
                event.setOrganizationId(context.organizationId());
            }
            tenantDatabaseContext.applyOrganization(event.getOrganizationId());
        }
        repository.save(event);
    }
}
