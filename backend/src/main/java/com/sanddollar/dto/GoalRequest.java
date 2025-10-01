package com.sanddollar.dto;

import com.sanddollar.entity.Goal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record GoalRequest(
    @NotNull
    Goal.GoalType goalType,

    @NotBlank
    String name,

    @NotNull
    @Positive
    BigDecimal targetAmount,

    @NotNull
    LocalDate targetDate,

    BigDecimal planMonthlyContribution,

    Map<String, Object> tripMetadata
) {}