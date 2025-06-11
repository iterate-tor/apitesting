package com.example.financialmanager.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalRequestDto(
    @NotBlank String goalName, // Renamed from 'name'
    @NotNull @Positive BigDecimal targetAmount,
    @NotNull LocalDate targetDate,
    LocalDate startDate // Optional, defaults to LocalDate.now() in service if null
    // currentAmount removed
) {}
