package com.psicogest.psicogest.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.model.entity.SubscriptionCycle;
import com.psicogest.psicogest.model.enums.SubscriptionStatus;
import com.psicogest.psicogest.repository.SubscriptionCycleRepository;

@Service
public class SubscriptionDelinquencyService {

    private final SubscriptionCycleRepository cycleRepository;
    private final Clock clock;

    public SubscriptionDelinquencyService(SubscriptionCycleRepository cycleRepository, Clock clock) {
        this.cycleRepository = cycleRepository;
        this.clock = clock;
    }

    @Transactional
    public void markPastDueCycles() {
        LocalDate today = LocalDate.now(clock);
        for (SubscriptionCycle cycle : cycleRepository.findBilledDue(today)) {
            int graceDays = cycle.getSubscription().getSubscriptionPlanVersion().getGraceDays();
            if (!today.isAfter(cycle.getReceivable().getDueDate().plusDays(graceDays))) {
                continue;
            }
            cycle.markPastDue(clock.instant());
            if (cycle.getSubscription().getStatus() == SubscriptionStatus.ACTIVE) {
                cycle.getSubscription().setStatus(SubscriptionStatus.PAST_DUE);
                cycle.getSubscription().setUpdatedAt(clock.instant());
            }
            cycleRepository.saveAndFlush(cycle);
        }
    }
}
