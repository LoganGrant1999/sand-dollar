package com.sanddollar.dto.aibudget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class GenerateBudgetRequest {
    
    @NotBlank(message = "Month is required")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Month must be in YYYY-MM format")
    private String month;
    
    @NotNull(message = "Goals are required")
    @Size(min = 1, max = 10, message = "Must have 1-10 goals")
    private List<@NotBlank(message = "Goal cannot be empty") @Size(max = 500, message = "Goal must be 500 characters or less") String> goals;
    
    @NotBlank(message = "Style is required")
    @Pattern(regexp = "aggressive|balanced|flexible", message = "Style must be 'aggressive', 'balanced', or 'flexible'")
    private String style;
    
    @Valid
    private BudgetConstraints constraints;
    
    @Size(max = 1000, message = "Notes must be 1000 characters or less")
    private String notes;
    
    public GenerateBudgetRequest() {}
    
    public GenerateBudgetRequest(String month, List<String> goals, String style, BudgetConstraints constraints, String notes) {
        this.month = month;
        this.goals = goals;
        this.style = style;
        this.constraints = constraints;
        this.notes = notes;
    }
    
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    
    public List<String> getGoals() { return goals; }
    public void setGoals(List<String> goals) { this.goals = goals; }
    
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    
    public BudgetConstraints getConstraints() { return constraints; }
    public void setConstraints(BudgetConstraints constraints) { this.constraints = constraints; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public static class BudgetConstraints {
        @Size(max = 20, message = "Cannot have more than 20 categories to keep")
        private List<@NotBlank(message = "Category name cannot be empty") String> mustKeepCategories;
        
        private Map<@NotBlank(message = "Category name cannot be empty") String, 
                    @NotNull(message = "Cap amount is required") @PositiveOrZero(message = "Cap amount must be positive or zero") BigDecimal> categoryCaps;
        
        public BudgetConstraints() {}
        
        public BudgetConstraints(List<String> mustKeepCategories, Map<String, BigDecimal> categoryCaps) {
            this.mustKeepCategories = mustKeepCategories;
            this.categoryCaps = categoryCaps;
        }
        
        public List<String> getMustKeepCategories() { return mustKeepCategories; }
        public void setMustKeepCategories(List<String> mustKeepCategories) { this.mustKeepCategories = mustKeepCategories; }
        
        public Map<String, BigDecimal> getCategoryCaps() { return categoryCaps; }
        public void setCategoryCaps(Map<String, BigDecimal> categoryCaps) { this.categoryCaps = categoryCaps; }
    }
}