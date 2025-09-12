package com.sanddollar.dto;

import java.time.Instant;

public record BalancesResponse(
    Long totalAvailableCents,
    Instant asOf
) {}