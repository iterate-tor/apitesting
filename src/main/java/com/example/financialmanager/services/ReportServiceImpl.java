package com.example.financialmanager.services;

import com.example.financialmanager.dtos.CategoryTotalDto;
import com.example.financialmanager.dtos.MonthlyReportDto;
import com.example.financialmanager.dtos.YearlyReportDto;
import com.example.financialmanager.entities.TransactionType;
import com.example.financialmanager.entities.User;
import com.example.financialmanager.repositories.TransactionRepository;
import com.example.financialmanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode; // Good practice for financial calculations
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap; // For maps
import java.util.List;
import java.util.Map; // For maps

@Service
public class ReportServiceImpl implements ReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Autowired
    public ReportServiceImpl(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyReportDto getMonthlyReport(int year, int month, String userEmail) {
        User user = getUserByEmail(userEmail);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<CategoryTotalDto> categoryTotals = transactionRepository.getCategoryTotalsByUserAndDateRange(user, startDate, endDate);

        Map<String, BigDecimal> totalIncomeMap = new HashMap<>();
        Map<String, BigDecimal> totalExpensesMap = new HashMap<>();

        for (CategoryTotalDto dto : categoryTotals) {
            // Ensure amounts are scaled consistently, e.g., 2 decimal places
            BigDecimal scaledAmount = dto.totalAmount().setScale(2, RoundingMode.HALF_UP);
            if (dto.type() == TransactionType.INCOME) {
                totalIncomeMap.put(dto.category(), scaledAmount);
            } else if (dto.type() == TransactionType.EXPENSE) {
                totalExpensesMap.put(dto.category(), scaledAmount);
            }
        }

        BigDecimal overallTotalIncome = totalIncomeMap.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal overallTotalExpenses = totalExpensesMap.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal netSavings = overallTotalIncome.subtract(overallTotalExpenses)
            .setScale(2, RoundingMode.HALF_UP);

        return new MonthlyReportDto(year, month, totalIncomeMap, totalExpensesMap, netSavings);
    }

    @Override
    @Transactional(readOnly = true)
    public YearlyReportDto getYearlyReport(int year, String userEmail) {
        User user = getUserByEmail(userEmail);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<CategoryTotalDto> categoryTotals = transactionRepository.getCategoryTotalsByUserAndDateRange(user, startDate, endDate);

        Map<String, BigDecimal> totalIncomeMap = new HashMap<>();
        Map<String, BigDecimal> totalExpensesMap = new HashMap<>();

        for (CategoryTotalDto dto : categoryTotals) {
            BigDecimal scaledAmount = dto.totalAmount().setScale(2, RoundingMode.HALF_UP);
            if (dto.type() == TransactionType.INCOME) {
                // If a category has both income and expenses over the year, this will sum them up per type.
                // The DTO from repo is (categoryName, sum(amount), type). So, if a category has multiple entries for a type (which it shouldn't due to GROUP BY),
                // this logic is fine. But if a category 'Consulting' is both INCOME and EXPENSE type, they are distinct entries in categoryTotals.
                totalIncomeMap.merge(dto.category(), scaledAmount, BigDecimal::add);
            } else if (dto.type() == TransactionType.EXPENSE) {
                totalExpensesMap.merge(dto.category(), scaledAmount, BigDecimal::add);
            }
        }

        // Ensure all map values are scaled, though merge should maintain scale if initial values are scaled.
        totalIncomeMap.replaceAll((k, v) -> v.setScale(2, RoundingMode.HALF_UP));
        totalExpensesMap.replaceAll((k, v) -> v.setScale(2, RoundingMode.HALF_UP));


        BigDecimal overallTotalIncome = totalIncomeMap.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal overallTotalExpenses = totalExpensesMap.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal netSavings = overallTotalIncome.subtract(overallTotalExpenses)
            .setScale(2, RoundingMode.HALF_UP);

        return new YearlyReportDto(year, totalIncomeMap, totalExpensesMap, netSavings);
    }
}
