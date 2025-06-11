package com.example.financialmanager.dtos;

import com.example.financialmanager.entities.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponseDto(
    UUID id,
    BigDecimal amount,
    LocalDate date,
    String category,
    String description,
    TransactionType type,
    UUID userId
) {}
