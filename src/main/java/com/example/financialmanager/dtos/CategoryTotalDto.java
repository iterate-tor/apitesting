package com.example.financialmanager.dtos;

import com.example.financialmanager.entities.TransactionType;
import java.math.BigDecimal;

public record CategoryTotalDto(
    String category,
    BigDecimal totalAmount,
    TransactionType type
) {
    // Constructor to be used by JPQL query
    public CategoryTotalDto(String category, BigDecimal totalAmount, TransactionType type) {
        this.category = category;
        this.totalAmount = totalAmount;
        this.type = type;
    }
}
