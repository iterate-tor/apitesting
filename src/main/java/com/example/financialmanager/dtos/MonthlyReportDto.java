package com.example.financialmanager.dtos;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyReportDto(
    int year,
    int month, // 1-12
    List<CategoryTotalDto> totalsByCategory,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netSavings
) {}
