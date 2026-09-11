package com.psicogest.psicogest.service;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Component;

@Component
public class MonthlyBillingPeriodCalculator {

    public BillingPeriod calculate(LocalDate start) {
        return calculate(start, start.getDayOfMonth());
    }

    public BillingPeriod calculate(LocalDate start, int anchorDay) {
        YearMonth nextMonth = YearMonth.from(start).plusMonths(1);
        LocalDate nextStart = resolveAnchor(nextMonth, anchorDay);
        return new BillingPeriod(start, nextStart.minusDays(1), nextStart);
    }

    public LocalDate resolveAnchor(YearMonth month, int anchorDay) {
        return month.atDay(Math.min(anchorDay, month.lengthOfMonth()));
    }
}
