package com.sanddollar.service;

import com.sanddollar.dto.BudgetWizardRequest;
import com.sanddollar.dto.BudgetWizardResponse;
import com.sanddollar.entity.Budget;
import com.sanddollar.entity.BudgetAllocation;
import com.sanddollar.repository.BudgetAllocationRepository;
import com.sanddollar.repository.BudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BudgetWizardService {
    
    private static final Logger logger = LoggerFactory.getLogger(BudgetWizardService.class);
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private BudgetAllocationRepository allocationRepository;
    
    /**
     * Get the current (latest) budget for a user
     */
    public Optional<BudgetWizardResponse> getCurrentBudget(Long userId) {
        logger.debug("Getting current budget for user: {}", userId);
        
        Optional<Budget> budgetOpt = budgetRepository.findCurrentBudgetByUserId(userId);
        if (budgetOpt.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(convertToResponse(budgetOpt.get()));
    }
    
    /**
     * Create a new budget
     */
    @Transactional
    public BudgetWizardResponse createBudget(Long userId, BudgetWizardRequest request) {
        logger.info("Creating budget for user: {} for {}/{}", userId, request.getMonth(), request.getYear());
        
        // Check if budget already exists for this month/year
        Optional<Budget> existingBudget = budgetRepository.findByUserIdAndMonthAndYear(userId, request.getMonth(), request.getYear());
        if (existingBudget.isPresent()) {
            throw new IllegalArgumentException("Budget already exists for " + request.getMonth() + "/" + request.getYear());
        }
        
        // Create budget entity
        Budget budget = new Budget(userId, request.getMonth(), request.getYear(), request.getIncome());
        budget = budgetRepository.save(budget);
        
        // Create allocations
        List<BudgetAllocation> allocations = new ArrayList<>();
        
        // Add fixed allocations
        for (BudgetWizardRequest.AllocationItem item : request.getFixed()) {
            allocations.add(new BudgetAllocation(budget, item.getName(), item.getAmount(), BudgetAllocation.Type.FIXED));
        }
        
        // Add variable allocations
        for (BudgetWizardRequest.AllocationItem item : request.getVariable()) {
            allocations.add(new BudgetAllocation(budget, item.getName(), item.getAmount(), BudgetAllocation.Type.VARIABLE));
        }
        
        allocationRepository.saveAll(allocations);
        budget.setAllocations(allocations);
        
        logger.info("Created budget {} with {} allocations", budget.getId(), allocations.size());
        return convertToResponse(budget);
    }
    
    /**
     * Update an existing budget
     */
    @Transactional
    public BudgetWizardResponse updateBudget(Long userId, UUID budgetId, BudgetWizardRequest request) {
        logger.info("Updating budget {} for user: {}", budgetId, userId);
        
        Optional<Budget> budgetOpt = budgetRepository.findByIdWithAllocations(budgetId);
        if (budgetOpt.isEmpty()) {
            throw new IllegalArgumentException("Budget not found: " + budgetId);
        }
        
        Budget budget = budgetOpt.get();
        if (!budget.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Budget does not belong to user");
        }
        
        // Update budget properties
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());
        budget.setIncome(request.getIncome());
        
        // Delete existing allocations
        allocationRepository.deleteByBudgetId(budgetId);
        
        // Create new allocations
        List<BudgetAllocation> allocations = new ArrayList<>();
        
        // Add fixed allocations
        for (BudgetWizardRequest.AllocationItem item : request.getFixed()) {
            allocations.add(new BudgetAllocation(budget, item.getName(), item.getAmount(), BudgetAllocation.Type.FIXED));
        }
        
        // Add variable allocations
        for (BudgetWizardRequest.AllocationItem item : request.getVariable()) {
            allocations.add(new BudgetAllocation(budget, item.getName(), item.getAmount(), BudgetAllocation.Type.VARIABLE));
        }
        
        allocationRepository.saveAll(allocations);
        budget.setAllocations(allocations);
        budget = budgetRepository.save(budget);
        
        logger.info("Updated budget {} with {} allocations", budgetId, allocations.size());
        return convertToResponse(budget);
    }
    
    /**
     * Get budget by ID
     */
    public Optional<BudgetWizardResponse> getBudgetById(Long userId, UUID budgetId) {
        logger.debug("Getting budget {} for user: {}", budgetId, userId);
        
        Optional<Budget> budgetOpt = budgetRepository.findByIdWithAllocations(budgetId);
        if (budgetOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Budget budget = budgetOpt.get();
        if (!budget.getUserId().equals(userId)) {
            return Optional.empty();
        }
        
        return Optional.of(convertToResponse(budget));
    }
    
    /**
     * Convert Budget entity to response DTO
     */
    private BudgetWizardResponse convertToResponse(Budget budget) {
        BudgetWizardResponse response = new BudgetWizardResponse();
        response.setId(budget.getId());
        response.setMonth(budget.getMonth());
        response.setYear(budget.getYear());
        response.setIncome(budget.getIncome());
        response.setCreatedAt(budget.getCreatedAt());
        response.setUpdatedAt(budget.getUpdatedAt());
        
        if (budget.getAllocations() != null) {
            // Separate fixed and variable allocations
            List<BudgetWizardResponse.AllocationResponse> fixedAllocations = budget.getAllocations().stream()
                .filter(allocation -> allocation.getType() == BudgetAllocation.Type.FIXED)
                .map(allocation -> new BudgetWizardResponse.AllocationResponse(allocation.getId(), allocation.getCategory(), allocation.getAmount()))
                .collect(Collectors.toList());
            
            List<BudgetWizardResponse.AllocationResponse> variableAllocations = budget.getAllocations().stream()
                .filter(allocation -> allocation.getType() == BudgetAllocation.Type.VARIABLE)
                .map(allocation -> new BudgetWizardResponse.AllocationResponse(allocation.getId(), allocation.getCategory(), allocation.getAmount()))
                .collect(Collectors.toList());
            
            response.setFixedAllocations(fixedAllocations);
            response.setVariableAllocations(variableAllocations);
        }
        
        return response;
    }
}