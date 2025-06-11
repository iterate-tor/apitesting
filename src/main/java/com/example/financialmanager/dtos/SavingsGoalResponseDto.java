package com.example.financialmanager.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SavingsGoalResponseDto(
    UUID id,
    String goalName, // Renamed from 'name'
    BigDecimal targetAmount,
    LocalDate targetDate,
    LocalDate startDate, // Added
    BigDecimal currentProgress, // Added (calculated value)
    Double progressPercentage, // Added (calculated value)
    BigDecimal remainingAmount // Added (calculated value)
    // UUID userId, // Removed
    // BigDecimal currentAmount // Removed (old stored value)
) {}
