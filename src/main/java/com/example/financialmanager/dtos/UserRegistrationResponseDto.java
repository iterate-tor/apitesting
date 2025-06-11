package com.example.financialmanager.dtos;

import java.util.UUID;

public record UserRegistrationResponseDto(
    String message,
    String userId // Assuming userId is UUID, so representing as String
) {
    public UserRegistrationResponseDto(String message, UUID userId) {
        this(message, userId.toString());
    }
}
