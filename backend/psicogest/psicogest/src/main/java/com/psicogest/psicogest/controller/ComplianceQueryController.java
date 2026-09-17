package com.psicogest.psicogest.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.psicogest.psicogest.dto.AuditEventResponse;
import com.psicogest.psicogest.dto.PrivacyRequestResponse;
import com.psicogest.psicogest.dto.SecuritySignalResponse;
import com.psicogest.psicogest.service.ComplianceQueryService;

@RestController
@RequestMapping("/api/v1/compliance")
@PreAuthorize("isAuthenticated()")
public class ComplianceQueryController {

    private final ComplianceQueryService service;

    public ComplianceQueryController(ComplianceQueryService service) {
        this.service = service;
    }

    @GetMapping("/privacy-requests")
    public List<PrivacyRequestResponse> privacyRequests() {
        return service.listPrivacyRequests();
    }

    @GetMapping("/audit-events")
    public List<AuditEventResponse> auditEvents() {
        return service.listAuditEvents();
    }

    @GetMapping("/security-signals")
    public List<SecuritySignalResponse> securitySignals() {
        return service.listSecuritySignals();
    }
}
