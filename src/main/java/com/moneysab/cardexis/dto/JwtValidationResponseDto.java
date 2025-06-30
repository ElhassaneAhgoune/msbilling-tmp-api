package com.moneysab.cardexis.dto;

/**
 * DTO for JWT validation response.
 */
public record JwtValidationResponseDto(
    boolean valid,
    String username,
    String role,
    String errorMessage
) {}