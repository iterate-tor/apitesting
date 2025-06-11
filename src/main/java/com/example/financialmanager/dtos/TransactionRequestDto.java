package com.example.financialmanager.dtos;

import com.example.financialmanager.entities.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID; // For categoryId

public record TransactionRequestDto(
    @NotNull @Positive BigDecimal amount,
    @NotNull @PastOrPresent LocalDate date,
    @NotNull UUID categoryId, // Changed from String category to UUID categoryId
    String description,
    @NotNull TransactionType type
) {}
