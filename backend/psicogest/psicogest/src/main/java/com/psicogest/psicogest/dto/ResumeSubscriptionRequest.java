package com.psicogest.psicogest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ResumeSubscriptionRequest(@NotNull LocalDate resumeDate) {
}
