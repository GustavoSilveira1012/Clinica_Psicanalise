package com.psicogest.psicogest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.psicogest.psicogest.dto.CancelSubscriptionRequest;
import com.psicogest.psicogest.dto.CreateSubscriptionRequest;
import com.psicogest.psicogest.dto.ResumeSubscriptionRequest;
import com.psicogest.psicogest.dto.SubscriptionResponse;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.SubscriptionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class PatientSubscriptionController {

    private final SubscriptionService service;
    private final SecurityActorFactory actorFactory;

    public PatientSubscriptionController(SubscriptionService service, SecurityActorFactory actorFactory) {
        this.service = service;
        this.actorFactory = actorFactory;
    }

    @PostMapping("/patients/{patientId}/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(
            @PathVariable Long patientId,
            @Valid @RequestBody CreateSubscriptionRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return service.create(patientId, request, actorFactory.from(authentication, servletRequest));
    }

    @GetMapping("/subscriptions/{subscriptionId}")
    public SubscriptionResponse find(@PathVariable UUID subscriptionId) {
        return service.find(subscriptionId);
    }

    @PostMapping("/subscriptions/{subscriptionId}/pause")
    public SubscriptionResponse pause(
            @PathVariable UUID subscriptionId,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        SecurityActor actor = actorFactory.from(authentication, servletRequest);
        return service.pause(subscriptionId, actor);
    }

    @PostMapping("/subscriptions/{subscriptionId}/resume")
    public SubscriptionResponse resume(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody ResumeSubscriptionRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        SecurityActor actor = actorFactory.from(authentication, servletRequest);
        return service.resume(subscriptionId, request, actor);
    }

    @PostMapping("/subscriptions/{subscriptionId}/cancel")
    public SubscriptionResponse cancel(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody CancelSubscriptionRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        SecurityActor actor = actorFactory.from(authentication, servletRequest);
        return service.cancel(subscriptionId, request, actor);
    }
}
