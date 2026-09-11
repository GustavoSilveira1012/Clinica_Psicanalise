package com.psicogest.psicogest.service;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.model.enums.ReceivableOriginType;
import com.psicogest.psicogest.repository.ReceivableRepository;

/** Entrada idempotente para o evento durável RECEIVABLE_PAID. */
@Component
public class SubscriptionCycleGrantProcessor {

    private final ReceivableRepository receivableRepository;
    private final SubscriptionEntitlementService entitlementService;

    public SubscriptionCycleGrantProcessor(
            ReceivableRepository receivableRepository,
            SubscriptionEntitlementService entitlementService
    ) {
        this.receivableRepository = receivableRepository;
        this.entitlementService = entitlementService;
    }

    @Transactional
    public void handleReceivablePaid(UUID receivableId) {
        var receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada"));
        if (receivable.getOriginType() != ReceivableOriginType.SUBSCRIPTION_CYCLE
                || receivable.getOriginId() == null) {
            return;
        }
        entitlementService.grantIfAllowed(
                receivable.getOriginId(), SubscriptionEntitlementService.Trigger.FULL_PAYMENT);
    }
}
