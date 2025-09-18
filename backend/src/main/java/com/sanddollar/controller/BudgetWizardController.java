package com.sanddollar.controller;

import com.sanddollar.dto.BudgetWizardRequest;
import com.sanddollar.dto.BudgetWizardResponse;
import com.sanddollar.service.BudgetWizardService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/budgets")
public class BudgetWizardController {
    
    private static final Logger logger = LoggerFactory.getLogger(BudgetWizardController.class);
    
    @Autowired
    private BudgetWizardService budgetWizardService;
    
    /**
     * Get current budget for user
     * GET /api/budgets/current
     */
    @GetMapping("/current")
    public ResponseEntity<BudgetWizardResponse> getCurrentBudget() {
        try {
            // For now, using a mock user ID. In real app, get from JWT/security context
            Long userId = 1L;
            
            logger.info("Getting current budget for user: {}", userId);
            
            Optional<BudgetWizardResponse> budget = budgetWizardService.getCurrentBudget(userId);
            
            if (budget.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(budget.get());
            
        } catch (Exception e) {
            logger.error("Error getting current budget", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create a new budget
     * POST /api/budgets
     */
    @PostMapping
    public ResponseEntity<BudgetWizardResponse> createBudget(@Valid @RequestBody BudgetWizardRequest request) {
        try {
            // For now, using a mock user ID. In real app, get from JWT/security context
            Long userId = 1L;
            
            logger.info("Creating budget for user: {}", userId);
            
            BudgetWizardResponse response = budgetWizardService.createBudget(userId, request);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request creating budget: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating budget", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update an existing budget
     * PUT /api/budgets/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetWizardResponse> updateBudget(@PathVariable UUID id, @Valid @RequestBody BudgetWizardRequest request) {
        try {
            // For now, using a mock user ID. In real app, get from JWT/security context
            Long userId = 1L;
            
            logger.info("Updating budget {} for user: {}", id, userId);
            
            BudgetWizardResponse response = budgetWizardService.updateBudget(userId, id, request);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request updating budget: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating budget {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get budget by ID
     * GET /api/budgets/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BudgetWizardResponse> getBudgetById(@PathVariable UUID id) {
        try {
            // For now, using a mock user ID. In real app, get from JWT/security context
            Long userId = 1L;
            
            logger.info("Getting budget {} for user: {}", id, userId);
            
            Optional<BudgetWizardResponse> budget = budgetWizardService.getBudgetById(userId, id);
            
            if (budget.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(budget.get());
            
        } catch (Exception e) {
            logger.error("Error getting budget {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}