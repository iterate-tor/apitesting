package com.example.financialmanager.dtos;

import java.math.BigDecimal;
import java.util.Map; // Changed from List<CategoryTotalDto>

public record YearlyReportDto(
    int year,
    Map<String, BigDecimal> totalIncome, // Changed from List and BigDecimal
    Map<String, BigDecimal> totalExpenses, // Changed from List and BigDecimal
    BigDecimal netSavings
    // Map<Integer, MonthlyReportDto> monthlyBreakdown // Optional, deferred - Kept comment
) {}
