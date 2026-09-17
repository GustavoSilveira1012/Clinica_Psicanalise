package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.saas.EntitlementResponse;
import com.psicogest.psicogest.dto.saas.SaasBillingResponse;
import com.psicogest.psicogest.service.saas.SaasBillingService;
import com.psicogest.psicogest.service.saas.SaasEntitlementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/organizations/{organizationId}")
@PreAuthorize("isAuthenticated()")
public class SaasBillingController {

    private final SaasBillingService billingService;
    private final SaasEntitlementService entitlementService;

    public SaasBillingController(
            SaasBillingService billingService,
            SaasEntitlementService entitlementService
    ) {
        this.billingService = billingService;
        this.entitlementService = entitlementService;
    }

    @GetMapping("/billing")
    public SaasBillingResponse billing(
            @PathVariable UUID organizationId,
            Authentication authentication
    ) {
        return billingService.summary(organizationId, authentication);
    }

    @GetMapping("/entitlements")
    public List<EntitlementResponse> entitlements(
            @PathVariable UUID organizationId,
            Authentication authentication
    ) {
        return entitlementService.list(authentication, organizationId);
    }
}
