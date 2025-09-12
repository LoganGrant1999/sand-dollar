package com.sanddollar.dto;

import jakarta.validation.constraints.NotNull;

public record PlaidLinkTokenRequest(
    @NotNull String userId
) {}