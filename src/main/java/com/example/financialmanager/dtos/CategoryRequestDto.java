package com.example.financialmanager.dtos;

import com.example.financialmanager.entities.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequestDto(
    @NotBlank String name,
    @NotNull TransactionType type
) {}
