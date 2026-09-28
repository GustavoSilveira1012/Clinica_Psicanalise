package com.psicogest.psicogest.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true")
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
