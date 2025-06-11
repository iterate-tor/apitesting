package com.example.financialmanager.dtos;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record UpdateTransactionRequestDto(
    // Amount is optional, but if present, must be positive
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be positive if provided")
    BigDecimal amount,

    String description
) {
    // Custom constructor or validation can be added if more complex rules are needed,
    // for example, to ensure at least one field is present.
    // For now, allowing both to be null means no update if both are null.
}
