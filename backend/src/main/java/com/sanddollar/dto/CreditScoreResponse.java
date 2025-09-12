package com.sanddollar.dto;

import java.time.Instant;

public record CreditScoreResponse(
    Integer score,
    String provider,
    Instant asOf
) {}