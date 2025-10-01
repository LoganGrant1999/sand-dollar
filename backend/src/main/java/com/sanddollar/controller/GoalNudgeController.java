package com.sanddollar.controller;

import com.sanddollar.entity.Transaction;
import com.sanddollar.security.UserPrincipal;
import com.sanddollar.service.GoalNudgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals/{goalId}/nudge")
@Profile("plaid")
public class GoalNudgeController {

    @Autowired
    private GoalNudgeService goalNudgeService;

    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearNudge(
            @PathVariable Long goalId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // Note: Should verify user ownership of goal here
        goalNudgeService.clearNudge(goalId);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<CandidateTransaction>> getCandidateContributions(
            @PathVariable Long goalId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<Transaction> candidates = goalNudgeService.getCandidateContributions(
            userPrincipal.getUserId(), goalId);

        List<CandidateTransaction> response = candidates.stream()
            .map(CandidateTransaction::fromTransaction)
            .toList();

        return ResponseEntity.ok(response);
    }

    public record CandidateTransaction(
        Long id,
        String name,
        String merchantName,
        Double amount,
        String date,
        String plaidTransactionId,
        Boolean isTransfer
    ) {
        public static CandidateTransaction fromTransaction(Transaction transaction) {
            return new CandidateTransaction(
                transaction.getId(),
                transaction.getName(),
                transaction.getMerchantName(),
                transaction.getAmountCents() != null ? transaction.getAmountCents() / 100.0 : 0.0,
                transaction.getDate().toString(),
                transaction.getPlaidTransactionId(),
                transaction.getIsTransfer()
            );
        }
    }
}