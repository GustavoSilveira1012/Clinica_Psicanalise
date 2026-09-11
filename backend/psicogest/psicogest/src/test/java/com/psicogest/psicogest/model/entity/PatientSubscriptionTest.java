package com.psicogest.psicogest.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.psicogest.psicogest.model.enums.SubscriptionStatus;

class PatientSubscriptionTest {

    @Test
    void futureSubscriptionStartsPendingAndUsesOriginalAnchor() {
        Instant now = LocalDate.of(2027, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC);
        SubscriptionPlanVersion version = new SubscriptionPlanVersion();
        Patient patient = new Patient();

        PatientSubscription subscription = PatientSubscription.create(
                UUID.randomUUID(), patient, UUID.randomUUID(), version,
                LocalDate.of(2027, 1, 31), now);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PENDING_START);
        assertThat(subscription.getBillingAnchorDay()).isEqualTo(31);
        assertThat(subscription.getNextCycleStart()).isEqualTo(LocalDate.of(2027, 1, 31));
    }
}
