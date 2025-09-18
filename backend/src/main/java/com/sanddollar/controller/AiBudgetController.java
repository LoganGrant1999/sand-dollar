package com.sanddollar.controller;

import com.sanddollar.dto.aibudget.*;
import com.sanddollar.service.AiBudgetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai/budget")
@Validated
public class AiBudgetController {

    @Autowired(required = false)
    private AiBudgetService aiBudgetService;
    
    @PostMapping("/snapshot")
    public ResponseEntity<?> getFinancialSnapshot() {
        if (aiBudgetService == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "AiBudgetService not available for local profile");
            return ResponseEntity.ok(errorResponse);
        }

        try {
            FinancialSnapshotResponse response = aiBudgetService.getFinancialSnapshot();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error generating financial snapshot: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    // @PostMapping("/generate")
    // public ResponseEntity<GenerateBudgetResponse> generateBudget(@Valid @RequestBody GenerateBudgetRequest request) {
    //     GenerateBudgetResponse response = aiBudgetService.generateBudget(request);
    //     return ResponseEntity.ok(response);
    // }
    //
    // @PostMapping("/accept")
    // public ResponseEntity<AcceptBudgetResponse> acceptBudget(@Valid @RequestBody AcceptBudgetRequest request) {
    //     AcceptBudgetResponse response = aiBudgetService.acceptBudget(request);
    //     return ResponseEntity.ok(response);
    // }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "AiBudgetController is working");
        response.put("profile", "local");
        return ResponseEntity.ok(response);
    }
}