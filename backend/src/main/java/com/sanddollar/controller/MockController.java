package com.sanddollar.controller;

import com.sanddollar.dto.BudgetPrefillResponse;
import com.sanddollar.entity.Transaction;
import com.sanddollar.service.MockPlaidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mock")
public class MockController {
    
    private static final Logger logger = LoggerFactory.getLogger(MockController.class);
    
    @Autowired
    private MockPlaidService mockPlaidService;
    
    
    /**
     * Get recent transactions for mock display
     * GET /api/mock/transactions?sinceDays=90
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getRecentTransactions(
            @RequestParam(defaultValue = "90") int sinceDays) {
        try {
            // For now, using a mock user ID. In real app, get from JWT/security context
            Long userId = 1L;
            
            logger.info("Getting recent transactions for user: {}, sinceDays: {}", userId, sinceDays);
            
            List<Transaction> transactions = mockPlaidService.getRecentTransactions(userId, sinceDays);
            
            return ResponseEntity.ok(transactions);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request for transactions: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error getting recent transactions", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get budget prefill suggestions based on transaction analysis
     * GET /api/mock/budget/prefill
     */
    @GetMapping("/budget/prefill")
    @Cacheable(value = "budgetPrefill", key = "#root.methodName", unless = "#result.body == null")
    public ResponseEntity<BudgetPrefillResponse> getBudgetPrefill() {
        try {
            // For now, using a mock user ID. In real app, get from JWT/security context
            Long userId = 1L;
            
            logger.info("Getting budget prefill for user: {}", userId);
            
            BudgetPrefillResponse prefillData = mockPlaidService.analyzeBudgetPrefill(userId);
            
            return ResponseEntity.ok(prefillData);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request for budget prefill: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error getting budget prefill", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get active budget for mock user
     * GET /api/mock/budget/active
     */
    @GetMapping("/budget/active")
    public ResponseEntity<?> getActiveBudget() {
        try {
            logger.info("Getting active budget for mock user");
            
            // Return simple mock budget data
            Map<String, Object> mockBudget = Map.of(
                "message", "No active budget plan found"
            );
            
            return ResponseEntity.ok(mockBudget);
            
        } catch (Exception e) {
            logger.error("Error getting active budget", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get budget history for mock user
     * GET /api/mock/budget/history?limit=6
     */
    @GetMapping("/budget/history")
    public ResponseEntity<?> getBudgetHistory(@RequestParam(defaultValue = "6") int limit) {
        try {
            logger.info("Getting budget history for mock user, limit: {}", limit);
            
            // Return empty history for now
            List<Map<String, Object>> mockHistory = List.of();
            
            return ResponseEntity.ok(mockHistory);
            
        } catch (Exception e) {
            logger.error("Error getting budget history", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}