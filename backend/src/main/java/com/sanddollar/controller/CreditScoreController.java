package com.sanddollar.controller;

import com.sanddollar.dto.CreditScoreResponse;
import com.sanddollar.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/credit-score")
public class CreditScoreController {

    @GetMapping
    public ResponseEntity<?> getCreditScore(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            // Mock credit score for MVP
            CreditScoreResponse response = new CreditScoreResponse(
                720,
                "MockCredit",
                Instant.now()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get credit score: " + e.getMessage()));
        }
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connectCreditProvider(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        // Placeholder for credit provider connection
        return ResponseEntity.ok(Map.of(
            "message", "Credit provider connection feature coming soon!",
            "status", "placeholder"
        ));
    }
}