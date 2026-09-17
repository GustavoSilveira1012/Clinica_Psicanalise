package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.saas.OrganizationMembershipResponse;
import com.psicogest.psicogest.service.saas.OrganizationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/organization-invites")
public class OrganizationInviteController {

    private final OrganizationService organizationService;

    public OrganizationInviteController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping("/{token}/accept")
    @PreAuthorize("isAuthenticated()")
    public OrganizationMembershipResponse accept(
            @PathVariable String token,
            Authentication authentication
    ) {
        return organizationService.acceptInvite(token, authentication);
    }
}
