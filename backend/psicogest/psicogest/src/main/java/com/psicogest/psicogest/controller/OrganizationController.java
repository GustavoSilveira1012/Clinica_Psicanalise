package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.saas.CreateOrganizationInviteRequest;
import com.psicogest.psicogest.dto.saas.CreateOrganizationRequest;
import com.psicogest.psicogest.dto.saas.OnboardingResponse;
import com.psicogest.psicogest.dto.saas.OrganizationInviteResponse;
import com.psicogest.psicogest.dto.saas.OrganizationMembershipResponse;
import com.psicogest.psicogest.dto.saas.OrganizationResponse;
import com.psicogest.psicogest.dto.saas.UpdateOnboardingRequest;
import com.psicogest.psicogest.service.saas.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/organizations")
@PreAuthorize("isAuthenticated()")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public List<OrganizationMembershipResponse> mine(Authentication authentication) {
        return organizationService.mine(authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(
            @Valid @RequestBody CreateOrganizationRequest request,
            Authentication authentication
    ) {
        return organizationService.create(request, authentication);
    }

    @GetMapping("/{organizationId}/members")
    public List<OrganizationMembershipResponse> members(
            @PathVariable UUID organizationId,
            Authentication authentication
    ) {
        return organizationService.members(organizationId, authentication);
    }

    @PostMapping("/{organizationId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationInviteResponse invite(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateOrganizationInviteRequest request,
            Authentication authentication
    ) {
        return organizationService.invite(organizationId, request, authentication);
    }

    @PatchMapping("/{organizationId}/onboarding")
    public OnboardingResponse updateOnboarding(
            @PathVariable UUID organizationId,
            @Valid @RequestBody UpdateOnboardingRequest request,
            Authentication authentication
    ) {
        return organizationService.updateOnboarding(organizationId, request.step(), authentication);
    }

    @GetMapping("/{organizationId}/onboarding")
    public OnboardingResponse onboarding(
            @PathVariable UUID organizationId,
            Authentication authentication
    ) {
        return organizationService.onboarding(organizationId, authentication);
    }

    @PostMapping("/{organizationId}/ownership/{membershipId}")
    public OrganizationMembershipResponse transferOwnership(
            @PathVariable UUID organizationId,
            @PathVariable UUID membershipId,
            Authentication authentication
    ) {
        return organizationService.transferOwnership(organizationId, membershipId, authentication);
    }

    @DeleteMapping("/{organizationId}/members/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID organizationId,
            @PathVariable UUID membershipId,
            Authentication authentication
    ) {
        organizationService.removeMember(organizationId, membershipId, authentication);
    }
}
