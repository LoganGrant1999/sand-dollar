package com.sanddollar.service;

import com.sanddollar.dto.GoalRequest;
import com.sanddollar.dto.GoalResponse;
import com.sanddollar.entity.Goal;
import com.sanddollar.entity.GoalContribution;
import com.sanddollar.entity.User;
import com.sanddollar.repository.GoalRepository;
import com.sanddollar.repository.GoalContributionRepository;
import com.sanddollar.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GoalService {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalContributionRepository goalContributionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FeasibilityService feasibilityService;

    public List<GoalResponse> getUserGoals(Long userId) {
        List<Goal> goals = goalRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return goals.stream()
                .map(goal -> {
                    BigDecimal savedAmount = computeSavedAmount(goal.getId());
                    NudgeData nudgeData = computeNudgeData(goal, userId);
                    return GoalResponse.fromEntity(goal, savedAmount, nudgeData.nudgeSuggested(), nudgeData.nudgeAmount());
                })
                .collect(Collectors.toList());
    }

    public GoalResponse getGoalById(Long goalId, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));

        // Check ownership
        if (!goal.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied to goal");
        }

        BigDecimal savedAmount = computeSavedAmount(goalId);
        NudgeData nudgeData = computeNudgeData(goal, userId);
        return GoalResponse.fromEntity(goal, savedAmount, nudgeData.nudgeSuggested(), nudgeData.nudgeAmount());
    }

    @Transactional
    public GoalResponse createGoal(GoalRequest request, User user) {
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setGoalType(request.goalType());
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setTargetDate(request.targetDate());
        goal.setPlanMonthlyContribution(request.planMonthlyContribution());
        goal.setTripMetadata(request.tripMetadata());
        goal.setStatus(Goal.GoalStatus.ACTIVE);

        Goal savedGoal = goalRepository.save(goal);
        return GoalResponse.fromEntity(savedGoal, BigDecimal.ZERO);
    }

    @Transactional
    public GoalResponse updateGoal(Long goalId, GoalRequest request, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));

        // Check ownership
        if (!goal.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied to goal");
        }

        goal.setGoalType(request.goalType());
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setTargetDate(request.targetDate());
        goal.setPlanMonthlyContribution(request.planMonthlyContribution());
        goal.setTripMetadata(request.tripMetadata());

        Goal updatedGoal = goalRepository.save(goal);
        BigDecimal savedAmount = computeSavedAmount(goalId);
        return GoalResponse.fromEntity(updatedGoal, savedAmount);
    }

    @Transactional
    public void deleteGoal(Long goalId, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));

        // Check ownership
        if (!goal.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied to goal");
        }

        goalRepository.delete(goal);
    }

    public BigDecimal computeSavedAmount(Long goalId) {
        List<GoalContribution> contributions = goalContributionRepository.findByGoalIdOrderByCreatedAtAsc(goalId);
        return contributions.stream()
                .map(GoalContribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private NudgeData computeNudgeData(Goal goal, Long userId) {
        try {
            // Check if last payroll/inflow within 5 days
            LocalDate fiveDaysAgo = LocalDate.now().minusDays(5);
            Long recentIncomeCount = transactionRepository.sumIncomeByUserIdAndDateRange(userId, fiveDaysAgo, LocalDate.now());
            boolean hasRecentPayroll = recentIncomeCount != null && recentIncomeCount > 0;

            if (!hasRecentPayroll) {
                return new NudgeData(false, null);
            }

            // Check if no contribution in 10 days
            LocalDate tenDaysAgo = LocalDate.now().minusDays(10);
            boolean hasRecentContribution = goalContributionRepository.findByGoalIdOrderByCreatedAtDesc(goal.getId())
                .stream()
                .anyMatch(contrib -> contrib.getContributionDate().isAfter(tenDaysAgo));

            if (hasRecentContribution) {
                return new NudgeData(false, null);
            }

            // Get feasibility data to determine nudge amount
            com.sanddollar.dto.FeasibilityResult feasibility = feasibilityService.plan(userId, goal);
            BigDecimal nudgeAmount = feasibility.monthlyRequired();

            return new NudgeData(true, nudgeAmount);
        } catch (Exception e) {
            // If any error occurs, don't suggest nudge
            return new NudgeData(false, null);
        }
    }

    private record NudgeData(boolean nudgeSuggested, BigDecimal nudgeAmount) {}
}