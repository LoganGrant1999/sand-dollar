package com.sanddollar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record PlaidExchangeRequest(
    @NotBlank @JsonProperty("publicToken") String publicToken
) {}