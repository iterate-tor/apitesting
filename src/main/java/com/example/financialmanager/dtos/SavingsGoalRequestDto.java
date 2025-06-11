package com.example.financialmanager.dtos;

import jakarta.validation.constraints.Future; // Will remove if problematic, as per spec.
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalRequestDto(
    @NotBlank String name,
    @NotNull @Positive BigDecimal targetAmount,
    @NotNull LocalDate targetDate, // @Future is disallowed by spec, so not using it. Validation in service if needed.
    @PositiveOrZero BigDecimal currentAmount // Optional, defaults to 0 in service
) {}
