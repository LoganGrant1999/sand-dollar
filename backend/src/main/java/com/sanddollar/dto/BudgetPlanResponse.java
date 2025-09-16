package com.sanddollar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class BudgetPlanResponse {
    @JsonProperty("monthlyIncome")
    private BigDecimal monthlyIncome;
    
    @JsonProperty("fixedExpenses")
    private BigDecimal fixedExpenses;
    
    @JsonProperty("targetSavings")
    private BigDecimal targetSavings;
    
    @JsonProperty("variableTotal")
    private BigDecimal variableTotal;
    
    @JsonProperty("allocations")
    private List<Allocation> allocations;
    
    @JsonProperty("recommendations")
    private List<String> recommendations;

    public BudgetPlanResponse() {
    }

    public BudgetPlanResponse(BigDecimal monthlyIncome, BigDecimal fixedExpenses, 
                             BigDecimal targetSavings, BigDecimal variableTotal,
                             List<Allocation> allocations, List<String> recommendations) {
        this.monthlyIncome = monthlyIncome;
        this.fixedExpenses = fixedExpenses;
        this.targetSavings = targetSavings;
        this.variableTotal = variableTotal;
        this.allocations = allocations;
        this.recommendations = recommendations;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public BigDecimal getFixedExpenses() {
        return fixedExpenses;
    }

    public void setFixedExpenses(BigDecimal fixedExpenses) {
        this.fixedExpenses = fixedExpenses;
    }

    public BigDecimal getTargetSavings() {
        return targetSavings;
    }

    public void setTargetSavings(BigDecimal targetSavings) {
        this.targetSavings = targetSavings;
    }

    public BigDecimal getVariableTotal() {
        return variableTotal;
    }

    public void setVariableTotal(BigDecimal variableTotal) {
        this.variableTotal = variableTotal;
    }

    public List<Allocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<Allocation> allocations) {
        this.allocations = allocations;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
}