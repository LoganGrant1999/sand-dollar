package com.sanddollar.controller;

import com.sanddollar.dto.*;
import com.sanddollar.entity.Goal;
import com.sanddollar.entity.User;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.BudgetPlanService;
import com.sanddollar.service.FeasibilityService;
import com.sanddollar.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    @Autowired
    private GoalService goalService;

    @Autowired
    private FeasibilityService feasibilityService;

    @Autowired
    private BudgetPlanService budgetPlanService;

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getUserGoals(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<GoalResponse> goals = goalService.getUserGoals(userPrincipal.getUserId());
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getGoal(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            GoalResponse goal = goalService.getGoalById(id, userPrincipal.getUserId());
            return ResponseEntity.ok(goal);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalRequest request, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = new User();
        user.setId(userPrincipal.getUserId());

        GoalResponse createdGoal = goalService.createGoal(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGoal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(@PathVariable Long id, @Valid @RequestBody GoalRequest request, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            GoalResponse updatedGoal = goalService.updateGoal(id, request, userPrincipal.getUserId());
            return ResponseEntity.ok(updatedGoal);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            goalService.deleteGoal(id, userPrincipal.getUserId());
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/feasibility")
    public ResponseEntity<Map<String, Object>> getGoalFeasibility(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            GoalResponse goalResponse = goalService.getGoalById(id, userPrincipal.getUserId());

            // Create Goal entity for feasibility analysis
            Goal goal = new Goal();
            goal.setId(goalResponse.id());
            goal.setTargetAmount(goalResponse.targetAmount());
            goal.setTargetDate(goalResponse.targetDate());

            // Get feasibility analysis
            FeasibilityResult feasibility = feasibilityService.plan(userPrincipal.getUserId(), goal);

            // Get budget plan suggestion
            BudgetPlanSuggestion budgetPlan = budgetPlanService.buildBudgetPlan(userPrincipal.getUserId(), goal, feasibility);

            // Combine results with baseline numbers
            Map<String, Object> baselines = new HashMap<>();
            baselines.put("incomeAvg", feasibility.incomeAvg());
            baselines.put("fixedAvg", feasibility.fixedAvg());
            baselines.put("variableAvg", feasibility.variableAvg());

            Map<String, Object> response = new HashMap<>();
            response.put("feasibility", feasibility);
            response.put("budgetPlan", budgetPlan);
            response.put("baselines", baselines);

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}