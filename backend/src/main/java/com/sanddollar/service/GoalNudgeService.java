package com.sanddollar.service;

import com.sanddollar.entity.Account;
import com.sanddollar.entity.Goal;
import com.sanddollar.entity.GoalContribution;
import com.sanddollar.entity.Transaction;
import com.sanddollar.repository.AccountRepository;
import com.sanddollar.repository.GoalContributionRepository;
import com.sanddollar.repository.GoalRepository;
import com.sanddollar.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Profile("plaid")
@Transactional
public class GoalNudgeService {
    private static final Logger logger = LoggerFactory.getLogger(GoalNudgeService.class);
    private static final BigDecimal NUDGE_THRESHOLD = new BigDecimal("200.00"); // $200 minimum inflow to trigger nudge
    private static final int NUDGE_COOLDOWN_DAYS = 10; // Days between nudges

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalContributionRepository goalContributionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FeasibilityService feasibilityService;

    public void checkForNudgeOpportunities(Long userId) {
        logger.info("Checking for nudge opportunities for user {}", userId);

        try {
            // Get user's active goals
            List<Goal> activeGoals = goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Goal.GoalStatus.ACTIVE);
            if (activeGoals.isEmpty()) {
                logger.debug("No active goals found for user {}", userId);
                return;
            }

            // Check for recent large inflows (last 3 days)
            LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
            List<Transaction> recentInflows = getRecentLargeInflows(userId, threeDaysAgo);

            if (recentInflows.isEmpty()) {
                logger.debug("No recent large inflows found for user {}", userId);
                return;
            }

            logger.info("Found {} recent large inflows for user {}", recentInflows.size(), userId);

            // Process nudges for each goal
            for (Goal goal : activeGoals) {
                checkGoalForNudge(goal, userId);
            }

        } catch (Exception e) {
            logger.error("Error checking nudge opportunities for user " + userId, e);
        }
    }

    private void checkGoalForNudge(Goal goal, Long userId) {
        logger.debug("Checking nudge for goal {} ({})", goal.getId(), goal.getName());

        // Skip if goal already has a nudge
        if (Boolean.TRUE.equals(goal.getNudgeSuggested())) {
            logger.debug("Goal {} already has active nudge", goal.getId());
            return;
        }

        // Check if there's been a recent contribution to this goal
        LocalDate cooldownDate = LocalDate.now().minusDays(NUDGE_COOLDOWN_DAYS);
        boolean hasRecentContribution = goalContributionRepository
            .findByGoalIdOrderByCreatedAtDesc(goal.getId())
            .stream()
            .anyMatch(contrib -> contrib.getContributionDate().isAfter(cooldownDate));

        if (hasRecentContribution) {
            logger.debug("Goal {} has recent contribution within {} days", goal.getId(), NUDGE_COOLDOWN_DAYS);
            return;
        }

        // Calculate nudge amount based on feasibility
        BigDecimal nudgeAmount = calculateNudgeAmount(goal, userId);
        if (nudgeAmount == null || nudgeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.debug("No valid nudge amount calculated for goal {}", goal.getId());
            return;
        }

        // Activate the nudge
        goal.setNudgeSuggested(true);
        goal.setNudgeAmount(nudgeAmount);
        goal.setUpdatedAt(LocalDateTime.now());

        goalRepository.save(goal);
        logger.info("Activated nudge for goal {} ({}): {}", goal.getId(), goal.getName(), nudgeAmount);
    }

    private BigDecimal calculateNudgeAmount(Goal goal, Long userId) {
        try {
            // Get feasibility data for the goal
            var feasibilityResult = feasibilityService.plan(userId, goal);

            // Use monthly required as the nudge amount
            BigDecimal monthlyRequired = feasibilityResult.monthlyRequired();

            // Cap the nudge at reasonable limits (between $25 and $1000)
            BigDecimal minNudge = new BigDecimal("25.00");
            BigDecimal maxNudge = new BigDecimal("1000.00");

            if (monthlyRequired.compareTo(minNudge) < 0) {
                return minNudge;
            } else if (monthlyRequired.compareTo(maxNudge) > 0) {
                return maxNudge;
            }

            return monthlyRequired;

        } catch (Exception e) {
            logger.warn("Error calculating nudge amount for goal {}: {}", goal.getId(), e.getMessage());
            return new BigDecimal("100.00"); // Default fallback amount
        }
    }

    private List<Transaction> getRecentLargeInflows(Long userId, LocalDate since) {
        // Get user's cash accounts (checking/savings, not credit/loan)
        List<Account> cashAccounts = accountRepository.findByUserIdAndAccountType(userId, "depository");

        return transactionRepository.findByAccountUserIdAndAmountThresholdSince(
            userId,
            NUDGE_THRESHOLD.multiply(new BigDecimal("100")).longValue(), // Convert to cents
            since
        );
    }

    /**
     * Clear nudge for a goal (called when user acts on the nudge)
     */
    public void clearNudge(Long goalId) {
        Goal goal = goalRepository.findById(goalId).orElse(null);
        if (goal != null && Boolean.TRUE.equals(goal.getNudgeSuggested())) {
            goal.setNudgeSuggested(false);
            goal.setNudgeAmount(null);
            goal.setUpdatedAt(LocalDateTime.now());
            goalRepository.save(goal);
            logger.info("Cleared nudge for goal {} ({})", goal.getId(), goal.getName());
        }
    }

    /**
     * Get recent transactions that could be assigned as contributions
     */
    public List<Transaction> getCandidateContributions(Long userId, Long goalId) {
        LocalDate twoWeeksAgo = LocalDate.now().minusDays(14);

        // Find recent inflows that haven't been assigned to this goal yet
        return transactionRepository.findRecentInflowsForContribution(userId, goalId, twoWeeksAgo);
    }
}