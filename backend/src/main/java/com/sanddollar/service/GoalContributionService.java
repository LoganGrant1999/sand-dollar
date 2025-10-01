package com.sanddollar.service;

import com.sanddollar.dto.GoalContributionRequest;
import com.sanddollar.dto.GoalContributionResponse;
import com.sanddollar.entity.Goal;
import com.sanddollar.entity.GoalContribution;
import com.sanddollar.repository.GoalContributionRepository;
import com.sanddollar.repository.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class GoalContributionService {

    @Autowired
    private GoalContributionRepository goalContributionRepository;

    @Autowired
    private GoalRepository goalRepository;

    public GoalContributionResponse createManualContribution(Long goalId, GoalContributionRequest request, Long userId) {
        // Verify goal exists and belongs to user
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
            .orElseThrow(() -> new SecurityException("Goal not found or access denied"));

        GoalContribution contribution = new GoalContribution(
            goal,
            request.amount(),
            request.getEffectiveContributionDate(),
            request.description(),
            GoalContribution.ContributionSource.MANUAL
        );

        contribution = goalContributionRepository.save(contribution);
        return GoalContributionResponse.fromEntity(contribution);
    }

    @Transactional(readOnly = true)
    public List<GoalContributionResponse> getGoalContributions(Long goalId, Long userId) {
        // Verify goal exists and belongs to user
        goalRepository.findByIdAndUserId(goalId, userId)
            .orElseThrow(() -> new SecurityException("Goal not found or access denied"));

        return goalContributionRepository.findByGoalIdOrderByCreatedAtDesc(goalId)
            .stream()
            .map(GoalContributionResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getSavedAmount(Long goalId) {
        return goalContributionRepository.findByGoalIdOrderByCreatedAtAsc(goalId)
            .stream()
            .map(GoalContribution::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // For use by PlaidSyncService
    public GoalContribution createAutoContribution(Goal goal, BigDecimal amount, String description) {
        GoalContribution contribution = new GoalContribution(
            goal,
            amount,
            java.time.LocalDate.now(),
            description,
            GoalContribution.ContributionSource.AUTO
        );

        return goalContributionRepository.save(contribution);
    }
}