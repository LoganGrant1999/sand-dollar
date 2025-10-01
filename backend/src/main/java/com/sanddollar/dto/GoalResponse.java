package com.sanddollar.dto;

import com.sanddollar.entity.Goal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record GoalResponse(
    Long id,
    Goal.GoalType goalType,
    String name,
    BigDecimal targetAmount,
    LocalDate targetDate,
    BigDecimal planMonthlyContribution,
    Goal.GoalStatus status,
    Map<String, Object> tripMetadata,
    BigDecimal savedAmount,
    Double percentComplete,
    Boolean nudgeSuggested,
    BigDecimal nudgeAmount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static GoalResponse fromEntity(Goal goal, BigDecimal savedAmount) {
        return fromEntity(goal, savedAmount, false, null);
    }

    public static GoalResponse fromEntity(Goal goal, BigDecimal savedAmount, boolean nudgeSuggested, BigDecimal nudgeAmount) {
        double percentComplete = 0.0;
        if (savedAmount != null && goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentComplete = savedAmount.divide(goal.getTargetAmount(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100))
                .doubleValue();
            // Cap at 100%
            percentComplete = Math.min(percentComplete, 100.0);
        }

        return new GoalResponse(
            goal.getId(),
            goal.getGoalType(),
            goal.getName(),
            goal.getTargetAmount(),
            goal.getTargetDate(),
            goal.getPlanMonthlyContribution(),
            goal.getStatus(),
            goal.getTripMetadata(),
            savedAmount,
            percentComplete,
            nudgeSuggested,
            nudgeAmount,
            goal.getCreatedAt(),
            goal.getUpdatedAt()
        );
    }
}