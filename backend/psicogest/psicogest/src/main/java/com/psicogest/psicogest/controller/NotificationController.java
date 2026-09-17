package com.psicogest.psicogest.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.psicogest.psicogest.dto.NotificationDeliveryResponse;
import com.psicogest.psicogest.dto.NotificationPreferenceResponse;
import com.psicogest.psicogest.dto.NotificationPreferenceUpdateRequest;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.notification.NotificationQueryService;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationQueryService service;
    private final SecurityActorFactory actorFactory;

    public NotificationController(NotificationQueryService service, SecurityActorFactory actorFactory) {
        this.service = service;
        this.actorFactory = actorFactory;
    }

    @GetMapping("/deliveries")
    public List<NotificationDeliveryResponse> deliveries() {
        return service.listDeliveries();
    }

    @GetMapping("/preferences")
    public List<NotificationPreferenceResponse> preferences() {
        return service.listPreferences();
    }

    @PutMapping("/preferences/{id}")
    public ResponseEntity<NotificationPreferenceResponse> updatePreference(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationPreferenceUpdateRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(service.updatePreference(
                id,
                request.enabled(),
                actorFactory.from(authentication, servletRequest)));
    }
}
