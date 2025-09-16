package com.sanddollar.dto.aibudget;

import jakarta.validation.constraints.NotBlank;

public class AcceptBudgetResponse {
    
    @NotBlank(message = "Status is required")
    private String status;
    
    public AcceptBudgetResponse() {}
    
    public AcceptBudgetResponse(String status) {
        this.status = status;
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}