package com.sanddollar.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessage(
    @NotBlank String role, // "user" or "assistant"
    @NotBlank String content
) {}