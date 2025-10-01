package com.sanddollar.controller;

import com.sanddollar.dto.GoalContributionRequest;
import com.sanddollar.dto.GoalContributionResponse;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.GoalContributionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals/{goalId}/contributions")
public class GoalContributionController {

    @Autowired
    private GoalContributionService goalContributionService;

    @PostMapping
    public ResponseEntity<GoalContributionResponse> createContribution(
            @PathVariable Long goalId,
            @Valid @RequestBody GoalContributionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            GoalContributionResponse response = goalContributionService.createManualContribution(
                goalId, request, userPrincipal.getUserId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<GoalContributionResponse>> getContributions(
            @PathVariable Long goalId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            List<GoalContributionResponse> contributions = goalContributionService.getGoalContributions(
                goalId, userPrincipal.getUserId()
            );
            return ResponseEntity.ok(contributions);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}