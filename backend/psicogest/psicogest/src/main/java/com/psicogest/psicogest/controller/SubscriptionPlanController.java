package com.psicogest.psicogest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.psicogest.psicogest.dto.CreateSubscriptionPlanRequest;
import com.psicogest.psicogest.dto.CreateSubscriptionPlanVersionRequest;
import com.psicogest.psicogest.dto.SubscriptionPlanVersionResponse;
import com.psicogest.psicogest.model.entity.SubscriptionPlanVersion;
import com.psicogest.psicogest.service.SubscriptionPlanService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/subscription-plans")
public class SubscriptionPlanController {

    private final SubscriptionPlanService service;

    public SubscriptionPlanController(SubscriptionPlanService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UUID create(@Valid @RequestBody CreateSubscriptionPlanRequest request) {
        return service.createPlan(request).getId();
    }

    @PostMapping("/{planId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionPlanVersionResponse createVersion(
            @PathVariable UUID planId,
            @Valid @RequestBody CreateSubscriptionPlanVersionRequest request
    ) {
        return toResponse(service.createVersion(planId, request));
    }

    @PostMapping("/versions/{versionId}/publish")
    public SubscriptionPlanVersionResponse publish(@PathVariable UUID versionId) {
        return toResponse(service.publishVersion(versionId));
    }

    private SubscriptionPlanVersionResponse toResponse(SubscriptionPlanVersion v) {
        return new SubscriptionPlanVersionResponse(
                v.getId(), v.getSubscriptionPlan().getId(), v.getVersion(), v.getStatus(),
                v.getEntitlementPackageVersion().getId(), v.getCyclePrice(), v.getCurrency(),
                v.getBillingInterval(), v.getIntervalCount(), v.getGrantPolicy(), v.getRolloverPolicy(),
                v.getCancellationPolicy(), v.getGraceDays(), v.getEffectiveFrom());
    }
}
