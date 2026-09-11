package com.psicogest.psicogest.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;
import com.psicogest.psicogest.infrastructure.payment.provider.RecurringChargeCommand;
import com.psicogest.psicogest.infrastructure.payment.provider.RecurringChargeResult;
import com.psicogest.psicogest.infrastructure.payment.provider.RecurringChargeStatus;
import com.psicogest.psicogest.infrastructure.payment.provider.RecurringPaymentProvider;
import com.psicogest.psicogest.infrastructure.payment.provider.RecurringPaymentProviderRegistry;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.SubscriptionChargeAttempt;
import com.psicogest.psicogest.model.entity.SubscriptionCycle;
import com.psicogest.psicogest.model.entity.SubscriptionPaymentMandate;
import com.psicogest.psicogest.model.enums.SubscriptionChargeAttemptStatus;
import com.psicogest.psicogest.repository.PaymentRepository;
import com.psicogest.psicogest.repository.SubscriptionChargeAttemptRepository;
import com.psicogest.psicogest.repository.SubscriptionCycleRepository;
import com.psicogest.psicogest.repository.SubscriptionPaymentMandateRepository;

@Service
public class RecurringChargeService {

    private final SubscriptionCycleRepository cycleRepository;
    private final SubscriptionPaymentMandateRepository mandateRepository;
    private final SubscriptionChargeAttemptRepository attemptRepository;
    private final PaymentRepository paymentRepository;
    private final RecurringPaymentProviderRegistry providerRegistry;
    private final Clock clock;

    public RecurringChargeService(
            SubscriptionCycleRepository cycleRepository,
            SubscriptionPaymentMandateRepository mandateRepository,
            SubscriptionChargeAttemptRepository attemptRepository,
            PaymentRepository paymentRepository,
            RecurringPaymentProviderRegistry providerRegistry,
            Clock clock
    ) {
        this.cycleRepository = cycleRepository;
        this.mandateRepository = mandateRepository;
        this.attemptRepository = attemptRepository;
        this.paymentRepository = paymentRepository;
        this.providerRegistry = providerRegistry;
        this.clock = clock;
    }

    public RecurringChargeResult charge(UUID cycleId) {
        PreparedCharge prepared = prepare(cycleId);
        try {
            RecurringPaymentProvider provider = providerRegistry.get(PaymentProviderType.valueOf(prepared.attempt().getProvider()));
            RecurringChargeResult result = provider.charge(prepared.command());
            complete(prepared.attempt().getId(), result);
            return result;
        } catch (RuntimeException exception) {
            markReconciliationRequired(prepared.attempt().getId(), exception.getClass().getSimpleName());
            return new RecurringChargeResult(RecurringChargeStatus.RECONCILIATION_REQUIRED, null,
                    exception.getClass().getSimpleName());
        }
    }

    @Transactional
    protected PreparedCharge prepare(UUID cycleId) {
        SubscriptionCycle cycle = cycleRepository.findByIdForUpdate(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Ciclo não encontrado"));
        var statuses = List.of(
                SubscriptionChargeAttemptStatus.PENDING,
                SubscriptionChargeAttemptStatus.PROCESSING,
                SubscriptionChargeAttemptStatus.AWAITING_PROVIDER,
                SubscriptionChargeAttemptStatus.CONFIRMED);
        var open = attemptRepository.findOpenByCycleId(cycleId, statuses);
        if (open.isPresent()) {
            SubscriptionChargeAttempt existing = open.get();
            SubscriptionPaymentMandate mandate = mandateRepository.findActiveBySubscriptionId(
                    cycle.getSubscription().getId()).orElseThrow(() -> new IllegalStateException("Mandato ativo não encontrado"));
            return new PreparedCharge(existing, new RecurringChargeCommand(
                    cycle.getSubscription().getId(), cycleId, cycle.getBilledAmount(), cycle.getCurrency(),
                    mandate.getProviderCustomerReference(), mandate.getProviderPaymentMethodReference(), existing.getIdempotencyKey()));
        }
        SubscriptionPaymentMandate mandate = mandateRepository.findActiveBySubscriptionId(cycle.getSubscription().getId())
                .orElseThrow(() -> new IllegalStateException("Mandato ativo não encontrado"));
        int nextAttempt = attemptRepository.findTopBySubscriptionCycleIdOrderByAttemptNumberDesc(cycleId)
                .map(a -> a.getAttemptNumber() + 1).orElse(1);
        String key = "SUBSCRIPTION_CHARGE:" + cycle.getSubscription().getId() + ":" + cycleId + ":" + nextAttempt;
        Instant now = clock.instant();
        SubscriptionChargeAttempt attempt = attemptRepository.saveAndFlush(SubscriptionChargeAttempt.builder()
                .id(UUID.randomUUID())
                .subscriptionCycle(cycle)
                .provider(mandate.getProvider())
                .idempotencyKey(key)
                .attemptNumber(nextAttempt)
                .status(SubscriptionChargeAttemptStatus.PROCESSING)
                .requestedAt(now)
                .createdAt(now)
                .build());
        return new PreparedCharge(attempt, new RecurringChargeCommand(
                cycle.getSubscription().getId(), cycleId, cycle.getBilledAmount(), cycle.getCurrency(),
                mandate.getProviderCustomerReference(), mandate.getProviderPaymentMethodReference(), key));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void complete(UUID attemptId, RecurringChargeResult result) {
        SubscriptionChargeAttempt attempt = attemptRepository.findByIdForUpdate(attemptId).orElseThrow();
        Instant now = clock.instant();
        attempt.setProviderTransactionId(result.providerTransactionId());
        attempt.setCompletedAt(now);
        attempt.setStatus(switch (result.status()) {
            case CONFIRMED -> SubscriptionChargeAttemptStatus.CONFIRMED;
            case PENDING -> SubscriptionChargeAttemptStatus.AWAITING_PROVIDER;
            case FAILED -> SubscriptionChargeAttemptStatus.FAILED;
            case RECONCILIATION_REQUIRED -> SubscriptionChargeAttemptStatus.RECONCILIATION_REQUIRED;
        });
        if (result.status() == RecurringChargeStatus.CONFIRMED || result.status() == RecurringChargeStatus.PENDING) {
            Payment payment = paymentRepository.save(Payment.builder()
                    .id(UUID.randomUUID())
                    .patient(attempt.getSubscriptionCycle().getSubscription().getPatient())
                    .amount(attempt.getSubscriptionCycle().getBilledAmount())
                    .currency(attempt.getSubscriptionCycle().getCurrency())
                    .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                    .status(Payment.PaymentStatus.PENDING)
                    .provider(attempt.getProvider())
                    .providerTransactionId(result.providerTransactionId())
                    .idempotencyKey(attempt.getIdempotencyKey())
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            attempt.setPayment(payment);
        }
        attemptRepository.saveAndFlush(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markReconciliationRequired(UUID attemptId, String failureCode) {
        SubscriptionChargeAttempt attempt = attemptRepository.findByIdForUpdate(attemptId).orElseThrow();
        attempt.setStatus(SubscriptionChargeAttemptStatus.RECONCILIATION_REQUIRED);
        attempt.setFailureCode(failureCode);
        attempt.setCompletedAt(clock.instant());
        attemptRepository.saveAndFlush(attempt);
    }

    protected record PreparedCharge(SubscriptionChargeAttempt attempt, RecurringChargeCommand command) {
    }
}
