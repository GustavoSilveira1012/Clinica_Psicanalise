package com.psicogest.psicogest.service;

import java.time.LocalDate;

public record BillingPeriod(LocalDate start, LocalDate end, LocalDate nextStart) {
}
