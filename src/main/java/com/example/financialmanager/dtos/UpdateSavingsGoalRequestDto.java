package com.example.financialmanager.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive; // Can use this as well
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateSavingsGoalRequestDto(
    // If present, must be positive. If null, not updated.
    @DecimalMin(value = "0.01", message = "Target amount must be positive if provided")
    BigDecimal targetAmount,

    // If null, not updated.
    LocalDate targetDate
) {}
