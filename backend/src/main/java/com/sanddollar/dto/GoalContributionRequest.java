package com.sanddollar.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalContributionRequest(
    @NotNull
    @Positive
    BigDecimal amount,

    LocalDate contributionDate,

    String description
) {
    // Use today's date if not provided
    public LocalDate getEffectiveContributionDate() {
        return contributionDate != null ? contributionDate : LocalDate.now();
    }
}