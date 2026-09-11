package com.psicogest.psicogest.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.psicogest.psicogest.repository.PatientSubscriptionRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SubscriptionCycleScheduler {

    private final PatientSubscriptionRepository subscriptionRepository;
    private final SubscriptionCycleService cycleService;
    private final Clock clock;

    public SubscriptionCycleScheduler(
            PatientSubscriptionRepository subscriptionRepository,
            SubscriptionCycleService cycleService,
            Clock clock
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.cycleService = cycleService;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.subscription.cycle-cron:0 0 * * * *}")
    public void generateDueCycles() {
        for (var id : subscriptionRepository.findIdsReadyForCycle(LocalDate.now(clock))) {
            try {
                cycleService.createNextCycle(id);
            } catch (RuntimeException exception) {
                log.error("Falha ao materializar ciclo de assinatura {}", id, exception);
            }
        }
    }
}
