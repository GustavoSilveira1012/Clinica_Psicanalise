package com.psicogest.psicogest.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.psicogest.psicogest.dto.SubscriptionListResponse;
import com.psicogest.psicogest.service.SubscriptionCatalogService;

@RestController
@RequestMapping("/api/v1/subscriptions")
@PreAuthorize("isAuthenticated()")
public class SubscriptionCatalogController {

    private final SubscriptionCatalogService service;

    public SubscriptionCatalogController(SubscriptionCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<SubscriptionListResponse> list() {
        return service.list();
    }
}
