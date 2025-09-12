package com.sanddollar.dto;

import jakarta.validation.constraints.NotBlank;

public record PlaidExchangeRequest(
    @NotBlank String publicToken
) {}