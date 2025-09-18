package com.sanddollar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email @JsonProperty("email") String email,
    @NotBlank @Size(min = 8, max = 100) @JsonProperty("password") String password,
    @NotBlank @Size(max = 100) @JsonProperty("firstName") String firstName,
    @NotBlank @Size(max = 100) @JsonProperty("lastName") String lastName
) {}