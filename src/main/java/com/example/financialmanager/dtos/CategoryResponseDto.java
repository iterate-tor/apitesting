package com.example.financialmanager.dtos;

import java.util.UUID;

public record CategoryResponseDto(
    UUID id,
    String name,
    UUID userId
) {}
