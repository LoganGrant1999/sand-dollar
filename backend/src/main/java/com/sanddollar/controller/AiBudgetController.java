package com.sanddollar.controller;

import com.sanddollar.dto.aibudget.*;
import com.sanddollar.service.AiBudgetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/budget")
@Validated
@CrossOrigin(origins = "http://localhost:5173")
public class AiBudgetController {
    
    @Autowired
    private AiBudgetService aiBudgetService;
    
    @PostMapping("/snapshot")
    public ResponseEntity<FinancialSnapshotResponse> getFinancialSnapshot() {
        FinancialSnapshotResponse response = aiBudgetService.getFinancialSnapshot();
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/generate")
    public ResponseEntity<GenerateBudgetResponse> generateBudget(@Valid @RequestBody GenerateBudgetRequest request) {
        GenerateBudgetResponse response = aiBudgetService.generateBudget(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/accept")
    public ResponseEntity<AcceptBudgetResponse> acceptBudget(@Valid @RequestBody AcceptBudgetRequest request) {
        AcceptBudgetResponse response = aiBudgetService.acceptBudget(request);
        return ResponseEntity.ok(response);
    }
}