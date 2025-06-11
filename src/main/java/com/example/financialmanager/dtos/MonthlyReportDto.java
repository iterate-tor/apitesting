package com.example.financialmanager.dtos;

import java.math.BigDecimal;
import java.util.Map; // Changed from List<CategoryTotalDto>

public record MonthlyReportDto(
    int year,
    int month, // 1-12
    Map<String, BigDecimal> totalIncome, // Changed from List and BigDecimal
    Map<String, BigDecimal> totalExpenses, // Changed from List and BigDecimal
    BigDecimal netSavings
) {}
