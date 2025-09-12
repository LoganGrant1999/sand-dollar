package com.sanddollar.dto;

public record AuthResponse(
    String accessToken,
    String tokenType,
    Long expiresIn,
    UserInfo user
) {
    public static record UserInfo(
        Long id,
        String email,
        String firstName,
        String lastName
    ) {}
}