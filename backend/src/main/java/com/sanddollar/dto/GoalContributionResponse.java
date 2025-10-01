package com.sanddollar.dto;

import com.sanddollar.entity.GoalContribution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GoalContributionResponse(
    Long id,
    BigDecimal amount,
    LocalDate contributionDate,
    String description,
    String source, // MANUAL or AUTO
    LocalDateTime createdAt
) {
    public static GoalContributionResponse fromEntity(GoalContribution contribution) {
        return new GoalContributionResponse(
            contribution.getId(),
            contribution.getAmount(),
            contribution.getContributionDate(),
            contribution.getDescription(),
            contribution.getSource().name(),
            contribution.getCreatedAt()
        );
    }
}