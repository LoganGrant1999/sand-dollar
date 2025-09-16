package com.sanddollar.service;

import com.sanddollar.dto.aibudget.FinancialSnapshotResponse;
import com.sanddollar.dto.aibudget.GenerateBudgetRequest;
import com.sanddollar.dto.aibudget.GenerateBudgetResponse;
import com.sanddollar.dto.aibudget.AcceptBudgetRequest;
import com.sanddollar.dto.aibudget.AcceptBudgetResponse;

public interface AiBudgetService {
    
    /**
     * Get financial snapshot for the current month for authenticated user
     */
    FinancialSnapshotResponse getFinancialSnapshot();
    
    /**
     * Generate AI-powered budget recommendations based on user goals and preferences
     */
    GenerateBudgetResponse generateBudget(GenerateBudgetRequest request);
    
    /**
     * Accept and persist the AI-generated budget targets
     */
    AcceptBudgetResponse acceptBudget(AcceptBudgetRequest request);
}