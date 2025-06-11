package com.example.financialmanager.dtos;

import com.example.financialmanager.entities.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.UUID;

// All fields are optional. Service layer will handle partial updates.
public record UpdateTransactionRequestDto(
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be positive if provided")
    BigDecimal amount, // If null, not updated. If present, must be positive.

    UUID categoryId, // If null, not updated.

    String description, // If null, not updated. Can be empty string to clear description.

    TransactionType type // If null, not updated.
) {}
