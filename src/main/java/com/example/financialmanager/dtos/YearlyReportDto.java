package com.example.financialmanager.dtos;

import java.math.BigDecimal;
import java.util.List;
// import java.util.Map; // Not using monthlyBreakdown for now

public record YearlyReportDto(
    int year,
    List<CategoryTotalDto> totalsByCategory, // Aggregated for the whole year
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netSavings
    // Map<Integer, MonthlyReportDto> monthlyBreakdown // Optional, deferred
) {}
