package com.psicogest.psicogest.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionDelinquencyScheduler {

    private final SubscriptionDelinquencyService service;

    public SubscriptionDelinquencyScheduler(SubscriptionDelinquencyService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.subscription.delinquency-cron:0 30 * * * *}")
    public void markPastDue() {
        service.markPastDueCycles();
    }
}
