package com.psicogest.psicogest.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class MonthlyBillingPeriodCalculatorTest {

    private final MonthlyBillingPeriodCalculator calculator = new MonthlyBillingPeriodCalculator();

    @Test
    void preservesAnchorDayAcrossShortMonthsWithoutDrift() {
        BillingPeriod january = calculator.calculate(LocalDate.of(2027, 1, 31), 31);
        BillingPeriod february = calculator.calculate(january.nextStart(), 31);
        BillingPeriod march = calculator.calculate(february.nextStart(), 31);

        assertThat(january.nextStart()).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(february.nextStart()).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(march.nextStart()).isEqualTo(LocalDate.of(2027, 4, 30));
        assertThat(january.end()).isEqualTo(LocalDate.of(2027, 2, 27));
    }

    @Test
    void handlesLeapYear() {
        BillingPeriod period = calculator.calculate(LocalDate.of(2028, 1, 31), 31);

        assertThat(period.nextStart()).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(period.end()).isEqualTo(LocalDate.of(2028, 2, 28));
    }
}
