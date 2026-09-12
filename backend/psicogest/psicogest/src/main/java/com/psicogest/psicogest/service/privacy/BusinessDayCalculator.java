package com.psicogest.psicogest.service.privacy;

import java.time.Instant;
import java.time.ZoneId;

public interface BusinessDayCalculator {

    Instant addBusinessDays(
            Instant from,
            int days,
            ZoneId zone
    );
}
