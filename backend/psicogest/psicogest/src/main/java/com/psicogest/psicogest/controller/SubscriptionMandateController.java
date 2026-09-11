package com.psicogest.psicogest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.psicogest.psicogest.dto.CreateSubscriptionMandateRequest;
import com.psicogest.psicogest.service.SubscriptionMandateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionMandateController {

    private final SubscriptionMandateService service;

    public SubscriptionMandateController(SubscriptionMandateService service) {
        this.service = service;
    }

    @PostMapping("/{subscriptionId}/payment-mandate")
    @ResponseStatus(HttpStatus.CREATED)
    public UUID create(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody CreateSubscriptionMandateRequest request
    ) {
        return service.create(subscriptionId, request).getId();
    }
}
