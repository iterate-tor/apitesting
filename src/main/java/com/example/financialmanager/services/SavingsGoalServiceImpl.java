package com.example.financialmanager.services;

import com.example.financialmanager.dtos.SavingsGoalRequestDto;
import com.example.financialmanager.dtos.SavingsGoalResponseDto;
import com.example.financialmanager.dtos.UpdateSavingsGoalRequestDto; // Added
import com.example.financialmanager.entities.SavingsGoal;
import com.example.financialmanager.entities.Transaction; // Added
import com.example.financialmanager.entities.TransactionType; // Added
import com.example.financialmanager.entities.User;
import com.example.financialmanager.repositories.SavingsGoalRepository;
import com.example.financialmanager.repositories.TransactionRepository; // Added
import com.example.financialmanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode; // Added
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SavingsGoalServiceImpl implements SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository; // Added

    @Autowired
    public SavingsGoalServiceImpl(SavingsGoalRepository savingsGoalRepository,
                                  UserRepository userRepository,
                                  TransactionRepository transactionRepository) { // Added
        this.savingsGoalRepository = savingsGoalRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository; // Added
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    // Helper record for progress calculation results
    private record GoalProgress(BigDecimal currentProgress, Double progressPercentage, BigDecimal remainingAmount) {}

    private GoalProgress calculateGoalProgress(SavingsGoal goal, User user) {
        LocalDate today = LocalDate.now();
        LocalDate effectiveStartDate = goal.getStartDate();
        LocalDate effectiveEndDate = today;

        if (goal.getTargetDate().isBefore(today)) {
            effectiveEndDate = goal.getTargetDate();
        }

        if (effectiveStartDate.isAfter(today) || effectiveStartDate.isAfter(effectiveEndDate) ) { // Also check if start is after effectiveEnd
            BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return new GoalProgress(zero, 0.0, goal.getTargetAmount().setScale(2, RoundingMode.HALF_UP));
        }

        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(user, effectiveStartDate, effectiveEndDate);

        BigDecimal totalIncomeForSavings = transactions.stream()
            // This logic assumes ALL income contributes to ALL savings goals.
            // A more advanced system might link specific income transactions or savings account contributions.
            // For now, using net change (income - expenses) during the period as "savings contribution".
            .filter(t -> t.getType() == TransactionType.INCOME)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpensesRelevantToSavings = transactions.stream()
            // Similarly, this assumes ALL expenses detract from ALL savings goals.
            .filter(t -> t.getType() == TransactionType.EXPENSE)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Using net change in the period as "current progress" contribution.
        // This interpretation might need refinement based on more specific requirements
        // e.g., if only specific "savings" category transactions should count.
        // For now, it's overall "net savings" during the goal's active period.
        BigDecimal currentProgress = totalIncomeForSavings.subtract(totalExpensesRelevantToSavings);
        // currentProgress = currentProgress.max(BigDecimal.ZERO); // Progress cannot be negative conceptually for a goal.

        BigDecimal targetAmount = goal.getTargetAmount();
        if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) { // Target amount must be positive
            return new GoalProgress(
                currentProgress.setScale(2, RoundingMode.HALF_UP),
                targetAmount.compareTo(BigDecimal.ZERO) == 0 && currentProgress.compareTo(BigDecimal.ZERO) >=0 ? 100.0 : 0.0, // 100% if target is 0 and progress is non-negative
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        double progressPercentage = currentProgress.divide(targetAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).doubleValue();
        progressPercentage = Math.max(0.0, Math.min(progressPercentage, 100.0)); // Cap between 0 and 100

        BigDecimal remainingAmount = targetAmount.subtract(currentProgress);
        // remainingAmount = remainingAmount.max(BigDecimal.ZERO); // Remaining cannot be negative.

        return new GoalProgress(
            currentProgress.setScale(2, RoundingMode.HALF_UP),
            progressPercentage,
            remainingAmount.setScale(2, RoundingMode.HALF_UP)
        );
    }

    // Updated convertToDto to accept User for progress calculation
    private SavingsGoalResponseDto convertToDto(SavingsGoal goal, User user) {
        GoalProgress progress = calculateGoalProgress(goal, user);
        return new SavingsGoalResponseDto(
            goal.getId(),
            goal.getName(),
            goal.getTargetAmount().setScale(2, RoundingMode.HALF_UP),
            goal.getTargetDate(),
            goal.getStartDate(),
            progress.currentProgress(),
            progress.progressPercentage(),
            progress.remainingAmount()
        );
    }

    @Override
    @Transactional
    public SavingsGoalResponseDto createGoal(SavingsGoalRequestDto requestDto, String userEmail) {
        User user = getUserByEmail(userEmail);

        if (!requestDto.targetDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target date must be in the future.");
        }

        LocalDate startDate = (requestDto.startDate() == null) ? LocalDate.now() : requestDto.startDate();
        if (startDate.isAfter(requestDto.targetDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be after target date.");
        }
        // Also ensure startDate is not in the past if that's a rule, though allowing past start date might be valid.
        // For now, we allow it.

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName(requestDto.goalName()); // DTO field is goalName
        goal.setTargetAmount(requestDto.targetAmount());
        goal.setTargetDate(requestDto.targetDate());
        goal.setStartDate(startDate);
        // currentAmount removed from entity

        SavingsGoal savedGoal = savingsGoalRepository.save(goal);
        return convertToDto(savedGoal, user); // Pass user to convertToDto
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsGoalResponseDto> getAllGoals(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<SavingsGoal> goals = savingsGoalRepository.findByUser(user);
        return goals.stream()
            .map(goal -> convertToDto(goal, user)) // Pass user
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SavingsGoalResponseDto getGoalById(UUID goalId, String userEmail) {
        User user = getUserByEmail(userEmail);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUser(goalId, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found or access denied"));
        return convertToDto(goal, user); // Pass user
    }

    @Override
    @Transactional
    public SavingsGoalResponseDto updateGoal(UUID goalId, UpdateSavingsGoalRequestDto requestDto, String userEmail) { // DTO type updated
        User user = getUserByEmail(userEmail);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUser(goalId, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found or access denied"));

        if (requestDto.targetAmount() != null) {
            goal.setTargetAmount(requestDto.targetAmount());
        }
        if (requestDto.targetDate() != null) {
            LocalDate newTargetDate = requestDto.targetDate();
            if (!newTargetDate.isAfter(LocalDate.now())) {
                 throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target date must be in the future.");
            }
            if (newTargetDate.isBefore(goal.getStartDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target date cannot be before the goal's start date.");
            }
            goal.setTargetDate(newTargetDate);
        }
        // Name and StartDate are not updatable via this DTO/endpoint as per spec

        SavingsGoal updatedGoal = savingsGoalRepository.save(goal);
        return convertToDto(updatedGoal, user); // Pass user
    }

    @Override
    @Transactional
    public void deleteGoal(UUID goalId, String userEmail) {
        User user = getUserByEmail(userEmail);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUser(goalId, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found or access denied"));
        savingsGoalRepository.delete(goal);
    }
}
