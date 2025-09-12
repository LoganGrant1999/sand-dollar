package com.sanddollar.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BudgetChatRequest(
    @NotNull @Valid List<ChatMessage> messages,
    BudgetConstraints constraints
) {}