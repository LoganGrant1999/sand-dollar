package com.sanddollar.dto.aibudget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AcceptBudgetRequest {
    
    @NotBlank(message = "Month is required")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Month must be in YYYY-MM format")
    private String month;
    
    @NotNull(message = "Targets by category is required")
    @Size(min = 1, message = "At least one category target is required")
    @Valid
    private List<GenerateBudgetResponse.CategoryTarget> targetsByCategory;
    
    public AcceptBudgetRequest() {}
    
    public AcceptBudgetRequest(String month, List<GenerateBudgetResponse.CategoryTarget> targetsByCategory) {
        this.month = month;
        this.targetsByCategory = targetsByCategory;
    }
    
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    
    public List<GenerateBudgetResponse.CategoryTarget> getTargetsByCategory() { return targetsByCategory; }
    public void setTargetsByCategory(List<GenerateBudgetResponse.CategoryTarget> targetsByCategory) { this.targetsByCategory = targetsByCategory; }
}