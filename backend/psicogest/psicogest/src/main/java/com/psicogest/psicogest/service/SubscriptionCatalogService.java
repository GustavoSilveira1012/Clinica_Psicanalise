package com.psicogest.psicogest.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.dto.SubscriptionListResponse;
import com.psicogest.psicogest.model.entity.PatientSubscription;
import com.psicogest.psicogest.repository.PatientSubscriptionRepository;

@Service
public class SubscriptionCatalogService {

    private final PatientSubscriptionRepository repository;

    public SubscriptionCatalogService(PatientSubscriptionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionListResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private SubscriptionListResponse toResponse(PatientSubscription subscription) {
        String planName = subscription.getSubscriptionPlanVersion()
                .getSubscriptionPlan()
                .getName();
        String patientName = subscription.getPatient().getUser().getName();
        String cycle = subscription.getSubscriptionPlanVersion().getBillingInterval()
                + " " + subscription.getSubscriptionPlanVersion().getIntervalCount();
        return new SubscriptionListResponse(
                subscription.getId(),
                subscription.getPatient().getId(),
                patientName,
                subscription.getSubscriptionPlanVersion().getId(),
                planName,
                cycle,
                subscription.getStatus(),
                subscription.getNextCycleStart(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getCreatedAt());
    }
}
