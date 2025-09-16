package com.sanddollar.dto.aibudget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class FinancialSnapshotResponse {
    
    @NotBlank(message = "Month is required")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Month must be in YYYY-MM format")
    private String month;
    
    @NotNull(message = "Income is required")
    @PositiveOrZero(message = "Income must be positive or zero")
    private BigDecimal income;
    
    @NotNull(message = "Actuals by category is required")
    @Valid
    private List<CategoryActual> actualsByCategory;
    
    @NotNull(message = "Totals is required")
    @Valid
    private FinancialTotals totals;
    
    private List<CategoryTarget> targetsByCategory;
    private Instant acceptedAt;

    public FinancialSnapshotResponse() {}
    
    public FinancialSnapshotResponse(String month, BigDecimal income, List<CategoryActual> actualsByCategory, FinancialTotals totals, List<CategoryTarget> targetsByCategory, Instant acceptedAt) {
        this.month = month;
        this.income = income;
        this.actualsByCategory = actualsByCategory;
        this.totals = totals;
        this.targetsByCategory = targetsByCategory;
        this.acceptedAt = acceptedAt;
    }
    
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    
    public BigDecimal getIncome() { return income; }
    public void setIncome(BigDecimal income) { this.income = income; }
    
    public List<CategoryActual> getActualsByCategory() { return actualsByCategory; }
    public void setActualsByCategory(List<CategoryActual> actualsByCategory) { this.actualsByCategory = actualsByCategory; }
    
    public FinancialTotals getTotals() { return totals; }
    public void setTotals(FinancialTotals totals) { this.totals = totals; }
    
    public List<CategoryTarget> getTargetsByCategory() { return targetsByCategory; }
    public void setTargetsByCategory(List<CategoryTarget> targetsByCategory) { this.targetsByCategory = targetsByCategory; }
    
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
    
    public static class CategoryActual {
        @NotBlank(message = "Category name is required")
        private String category;
        
        @NotNull(message = "Actual amount is required")
        @PositiveOrZero(message = "Actual amount must be positive or zero")
        private BigDecimal actual;
        
        @PositiveOrZero(message = "Target amount must be positive or zero")
        private BigDecimal target;
        
        public CategoryActual() {}
        
        public CategoryActual(String category, BigDecimal actual) {
            this.category = category;
            this.actual = actual;
        }
        
        public CategoryActual(String category, BigDecimal actual, BigDecimal target) {
            this.category = category;
            this.actual = actual;
            this.target = target;
        }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public BigDecimal getActual() { return actual; }
        public void setActual(BigDecimal actual) { this.actual = actual; }
        
        public BigDecimal getTarget() { return target; }
        public void setTarget(BigDecimal target) { this.target = target; }
    }
    
    public static class FinancialTotals {
        @NotNull(message = "Expenses total is required")
        @PositiveOrZero(message = "Expenses must be positive or zero")
        private BigDecimal expenses;
        
        @NotNull(message = "Savings total is required")
        @PositiveOrZero(message = "Savings must be positive or zero")
        private BigDecimal savings;
        
        @NotNull(message = "Net cash flow is required")
        private BigDecimal netCashFlow;
        
        public FinancialTotals() {}
        
        public FinancialTotals(BigDecimal expenses, BigDecimal savings, BigDecimal netCashFlow) {
            this.expenses = expenses;
            this.savings = savings;
            this.netCashFlow = netCashFlow;
        }
        
        public BigDecimal getExpenses() { return expenses; }
        public void setExpenses(BigDecimal expenses) { this.expenses = expenses; }
        
        public BigDecimal getSavings() { return savings; }
        public void setSavings(BigDecimal savings) { this.savings = savings; }
        
        public BigDecimal getNetCashFlow() { return netCashFlow; }
        public void setNetCashFlow(BigDecimal netCashFlow) { this.netCashFlow = netCashFlow; }
    }

    public static class CategoryTarget {
        @NotBlank(message = "Category name is required")
        private String category;
        
        @NotNull
        @PositiveOrZero
        private BigDecimal target;
        
        @NotBlank(message = "Reason is required")
        private String reason;
        
        public CategoryTarget() {}
        
        public CategoryTarget(String category, BigDecimal target, String reason) {
            this.category = category;
            this.target = target;
            this.reason = reason;
        }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public BigDecimal getTarget() { return target; }
        public void setTarget(BigDecimal target) { this.target = target; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
