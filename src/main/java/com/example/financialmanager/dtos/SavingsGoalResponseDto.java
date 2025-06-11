package com.example.financialmanager.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SavingsGoalResponseDto(
    UUID id,
    String name,
    BigDecimal targetAmount,
    BigDecimal currentAmount,
    LocalDate targetDate,
    UUID userId
) {}
