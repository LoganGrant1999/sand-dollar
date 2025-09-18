package com.sanddollar.controller;

import com.sanddollar.dto.BudgetAdjustmentRequest;
import com.sanddollar.dto.BudgetAdjustmentResponse;
import com.sanddollar.service.BudgetAdjustmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/budgets")
public class BudgetAdjustmentController {
    
    private static final Logger logger = LoggerFactory.getLogger(BudgetAdjustmentController.class);
    
    @Autowired
    private BudgetAdjustmentService budgetAdjustmentService;
    
    @PostMapping("/adjust")
    public ResponseEntity<BudgetAdjustmentResponse> adjustBudget(@RequestBody BudgetAdjustmentRequest request) {
        try {
            logger.info("Received budget adjustment request: {}", request.getInstruction());
            
            BudgetAdjustmentResponse response = budgetAdjustmentService.adjustBudget(request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing budget adjustment", e);
            
            BudgetAdjustmentResponse errorResponse = new BudgetAdjustmentResponse("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}