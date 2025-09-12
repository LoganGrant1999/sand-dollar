package com.sanddollar.dto;

public record BudgetChatResponse(
    String summaryText,
    Object plan // The saved budget plan JSON
) {}