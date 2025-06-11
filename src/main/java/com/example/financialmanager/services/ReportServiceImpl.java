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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

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

        List<CategoryTotalDto> totalsByCategory = transactionRepository.getCategoryTotalsByUserAndDateRange(user, startDate, endDate);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (CategoryTotalDto total : totalsByCategory) {
            if (total.type() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(total.totalAmount());
            } else if (total.type() == TransactionType.EXPENSE) {
                totalExpenses = totalExpenses.add(total.totalAmount());
            }
        }

        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        return new MonthlyReportDto(year, month, totalsByCategory, totalIncome, totalExpenses, netSavings);
    }

    @Override
    @Transactional(readOnly = true)
    public YearlyReportDto getYearlyReport(int year, String userEmail) {
        User user = getUserByEmail(userEmail);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<CategoryTotalDto> totalsByCategory = transactionRepository.getCategoryTotalsByUserAndDateRange(user, startDate, endDate);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (CategoryTotalDto total : totalsByCategory) {
            if (total.type() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(total.totalAmount());
            } else if (total.type() == TransactionType.EXPENSE) {
                totalExpenses = totalExpenses.add(total.totalAmount());
            }
        }

        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        return new YearlyReportDto(year, totalsByCategory, totalIncome, totalExpenses, netSavings);
    }
}
