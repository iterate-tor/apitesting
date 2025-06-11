package com.example.financialmanager.dtos;

import com.example.financialmanager.entities.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequestDto(
    @NotNull @Positive BigDecimal amount, // Amount should always be positive
    @NotNull @PastOrPresent LocalDate date,
    @NotBlank String category,
    String description,
    @NotNull TransactionType type // Type is now explicit
) {}
