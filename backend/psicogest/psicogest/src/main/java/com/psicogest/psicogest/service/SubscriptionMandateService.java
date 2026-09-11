package com.psicogest.psicogest.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.dto.CreateSubscriptionMandateRequest;
import com.psicogest.psicogest.model.entity.PatientSubscription;
import com.psicogest.psicogest.model.entity.SubscriptionPaymentMandate;
import com.psicogest.psicogest.model.enums.SubscriptionMandateStatus;
import com.psicogest.psicogest.repository.PatientSubscriptionRepository;
import com.psicogest.psicogest.repository.SubscriptionPaymentMandateRepository;

@Service
public class SubscriptionMandateService {

    private final PatientSubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentMandateRepository mandateRepository;
    private final Clock clock;

    public SubscriptionMandateService(
            PatientSubscriptionRepository subscriptionRepository,
            SubscriptionPaymentMandateRepository mandateRepository,
            Clock clock
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.mandateRepository = mandateRepository;
        this.clock = clock;
    }

    @Transactional
    public SubscriptionPaymentMandate create(UUID subscriptionId, CreateSubscriptionMandateRequest request) {
        PatientSubscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada"));
        mandateRepository.findActiveBySubscriptionId(subscriptionId).ifPresent(active -> {
            active.setStatus(SubscriptionMandateStatus.REVOKED);
            active.setRevokedAt(clock.instant());
            mandateRepository.saveAndFlush(active);
        });
        return mandateRepository.save(SubscriptionPaymentMandate.builder()
                .id(UUID.randomUUID())
                .subscription(subscription)
                .provider(request.provider().trim())
                .providerCustomerReference(request.providerCustomerReference())
                .providerPaymentMethodReference(request.providerPaymentMethodReference().trim())
                .status(SubscriptionMandateStatus.ACTIVE)
                .createdAt(clock.instant())
                .build());
    }
}
