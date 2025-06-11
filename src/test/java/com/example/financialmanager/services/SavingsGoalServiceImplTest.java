package com.example.financialmanager.services;

import com.example.financialmanager.dtos.SavingsGoalRequestDto;
import com.example.financialmanager.dtos.SavingsGoalResponseDto;
import com.example.financialmanager.dtos.UpdateSavingsGoalRequestDto;
import com.example.financialmanager.entities.SavingsGoal;
import com.example.financialmanager.entities.Transaction;
import com.example.financialmanager.entities.TransactionType;
import com.example.financialmanager.entities.User;
import com.example.financialmanager.repositories.SavingsGoalRepository;
import com.example.financialmanager.repositories.TransactionRepository;
import com.example.financialmanager.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceImplTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private SavingsGoalServiceImpl savingsGoalService;

    private User testUser;
    private UUID userId;
    private LocalDate today;
    private LocalDate futureDate;
    private LocalDate pastDate;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User("user@example.com", "password", "Test User", "123");
        testUser.setId(userId);

        today = LocalDate.now();
        futureDate = today.plusMonths(6);
        pastDate = today.minusMonths(1);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
    }

    private SavingsGoal createSampleGoal(BigDecimal targetAmount, LocalDate startDate, LocalDate targetDate) {
        SavingsGoal goal = new SavingsGoal(testUser, "Test Goal", targetAmount, targetDate, startDate);
        goal.setId(UUID.randomUUID());
        return goal;
    }

    // --- calculateGoalProgress (tested via other methods) ---

    // Helper for mocking transactions
    private List<Transaction> mockTransactions(BigDecimal income, BigDecimal expense, LocalDate date) {
        // For simplicity, category is not essential for this calculation logic directly
        Transaction incomeTx = new Transaction(testUser, income, date, null, "Income", TransactionType.INCOME);
        Transaction expenseTx = new Transaction(testUser, expense, date, null, "Expense", TransactionType.EXPENSE);
        return Arrays.asList(incomeTx, expenseTx);
    }

     private List<Transaction> mockSingleTransaction(BigDecimal amount, TransactionType type, LocalDate date) {
        return Collections.singletonList(new Transaction(testUser, amount, date, null, "Single Tx", type));
    }


    @Test
    void createGoal_success_withStartDate() {
        SavingsGoalRequestDto requestDto = new SavingsGoalRequestDto("Vacation", BigDecimal.valueOf(1000), futureDate, today);
        SavingsGoal savedGoal = new SavingsGoal(testUser, "Vacation", BigDecimal.valueOf(1000), futureDate, today);
        savedGoal.setId(UUID.randomUUID());

        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(savedGoal);
        // Assume no transactions for a new goal starting today for progress calculation
        when(transactionRepository.findByUserAndDateBetween(eq(testUser), eq(today), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        SavingsGoalResponseDto responseDto = savingsGoalService.createGoal(requestDto, testUser.getEmail());

        assertNotNull(responseDto);
        assertEquals("Vacation", responseDto.goalName());
        assertEquals(today, responseDto.startDate());
        assertEquals(0.0, responseDto.progressPercentage());
        assertEquals(BigDecimal.valueOf(1000.00).setScale(2), responseDto.remainingAmount());
        verify(savingsGoalRepository).save(any(SavingsGoal.class));
    }

    @Test
    void createGoal_success_nullStartDate_defaultsToToday() {
        SavingsGoalRequestDto requestDto = new SavingsGoalRequestDto("Gadget", BigDecimal.valueOf(500), futureDate, null);
        SavingsGoal savedGoal = new SavingsGoal(testUser, "Gadget", BigDecimal.valueOf(500), futureDate, today);
        savedGoal.setId(UUID.randomUUID());

        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(savedGoal);
        when(transactionRepository.findByUserAndDateBetween(eq(testUser), eq(today), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());


        SavingsGoalResponseDto responseDto = savingsGoalService.createGoal(requestDto, testUser.getEmail());
        assertEquals(today, responseDto.startDate());
    }

    @Test
    void createGoal_targetDateNotFuture_throwsException() {
        SavingsGoalRequestDto requestDto = new SavingsGoalRequestDto("Error Goal", BigDecimal.valueOf(100), today, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> savingsGoalService.createGoal(requestDto, testUser.getEmail()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Target date must be in the future"));
    }

    @Test
    void createGoal_startDateAfterTargetDate_throwsException() {
        SavingsGoalRequestDto requestDto = new SavingsGoalRequestDto("Error Goal", BigDecimal.valueOf(100), futureDate, futureDate.plusDays(1));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> savingsGoalService.createGoal(requestDto, testUser.getEmail()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Start date cannot be after target date"));
    }

    @Test
    void getGoalById_checkProgressCalculation_noTransactions() {
        SavingsGoal goal = createSampleGoal(BigDecimal.valueOf(1000), pastDate, futureDate);
        when(savingsGoalRepository.findByIdAndUser(goal.getId(), testUser)).thenReturn(Optional.of(goal));
        when(transactionRepository.findByUserAndDateBetween(testUser, pastDate, today)).thenReturn(Collections.emptyList());

        SavingsGoalResponseDto dto = savingsGoalService.getGoalById(goal.getId(), testUser.getEmail());

        assertEquals(BigDecimal.ZERO.setScale(2), dto.currentProgress());
        assertEquals(0.0, dto.progressPercentage());
        assertEquals(BigDecimal.valueOf(1000).setScale(2), dto.remainingAmount());
    }

    @Test
    void getGoalById_checkProgressCalculation_onlyIncome() {
        SavingsGoal goal = createSampleGoal(BigDecimal.valueOf(1000), pastDate, futureDate);
        when(savingsGoalRepository.findByIdAndUser(goal.getId(), testUser)).thenReturn(Optional.of(goal));
        when(transactionRepository.findByUserAndDateBetween(testUser, pastDate, today))
            .thenReturn(mockSingleTransaction(BigDecimal.valueOf(200), TransactionType.INCOME, pastDate.plusDays(5)));

        SavingsGoalResponseDto dto = savingsGoalService.getGoalById(goal.getId(), testUser.getEmail());

        assertEquals(BigDecimal.valueOf(200).setScale(2), dto.currentProgress());
        assertEquals(20.0, dto.progressPercentage());
        assertEquals(BigDecimal.valueOf(800).setScale(2), dto.remainingAmount());
    }

    @Test
    void getGoalById_checkProgressCalculation_onlyExpense() {
        SavingsGoal goal = createSampleGoal(BigDecimal.valueOf(1000), pastDate, futureDate);
        when(savingsGoalRepository.findByIdAndUser(goal.getId(), testUser)).thenReturn(Optional.of(goal));
        when(transactionRepository.findByUserAndDateBetween(testUser, pastDate, today))
            .thenReturn(mockSingleTransaction(BigDecimal.valueOf(50), TransactionType.EXPENSE, pastDate.plusDays(5)));

        SavingsGoalResponseDto dto = savingsGoalService.getGoalById(goal.getId(), testUser.getEmail());

        assertEquals(BigDecimal.valueOf(-50).setScale(2), dto.currentProgress()); // Progress is negative
        assertEquals(0.0, dto.progressPercentage()); // Percentage capped at 0
        assertEquals(BigDecimal.valueOf(1050).setScale(2), dto.remainingAmount()); // Remaining increases
    }


    @Test
    void getGoalById_checkProgressCalculation_mixedTransactions() {
        SavingsGoal goal = createSampleGoal(BigDecimal.valueOf(1000), pastDate, futureDate);
        when(savingsGoalRepository.findByIdAndUser(goal.getId(), testUser)).thenReturn(Optional.of(goal));
        when(transactionRepository.findByUserAndDateBetween(testUser, pastDate, today))
            .thenReturn(mockTransactions(BigDecimal.valueOf(300), BigDecimal.valueOf(100), pastDate.plusDays(10))); // Net 200

        SavingsGoalResponseDto dto = savingsGoalService.getGoalById(goal.getId(), testUser.getEmail());

        assertEquals(BigDecimal.valueOf(200).setScale(2), dto.currentProgress());
        assertEquals(20.0, dto.progressPercentage());
        assertEquals(BigDecimal.valueOf(800).setScale(2), dto.remainingAmount());
    }

    @Test
    void getGoalById_checkProgressCalculation_startDateInFuture() {
        SavingsGoal goal = createSampleGoal(BigDecimal.valueOf(1000), today.plusDays(1), futureDate); // Start date is tomorrow
        when(savingsGoalRepository.findByIdAndUser(goal.getId(), testUser)).thenReturn(Optional.of(goal));
        // transactionRepository mock for findByUserAndDateBetween shouldn't even be called if start date is future

        SavingsGoalResponseDto dto = savingsGoalService.getGoalById(goal.getId(), testUser.getEmail());

        assertEquals(BigDecimal.ZERO.setScale(2), dto.currentProgress());
        assertEquals(0.0, dto.progressPercentage());
        assertEquals(BigDecimal.valueOf(1000).setScale(2), dto.remainingAmount());
        verify(transactionRepository, never()).findByUserAndDateBetween(any(), any(), any());
    }

    @Test
    void getGoalById_checkProgressCalculation_targetAmountZero() {
        SavingsGoal goal = createSampleGoal(BigDecimal.ZERO, pastDate, futureDate);
        when(savingsGoalRepository.findByIdAndUser(goal.getId(), testUser)).thenReturn(Optional.of(goal));
        when(transactionRepository.findByUserAndDateBetween(testUser, pastDate, today))
            .thenReturn(mockSingleTransaction(BigDecimal.valueOf(50), TransactionType.INCOME, pastDate.plusDays(5)));

        SavingsGoalResponseDto dto = savingsGoalService.getGoalById(goal.getId(), testUser.getEmail());

        assertEquals(BigDecimal.valueOf(50).setScale(2), dto.currentProgress());
        assertEquals(100.0, dto.progressPercentage()); // Target is 0, progress is positive
        assertEquals(BigDecimal.ZERO.setScale(2), dto.remainingAmount());
    }

    @Test
    void getGoalById_checkProgressCalculation_progressExceedsTarget() {
        SavingsGoal goal = createSampleGoal(BigDecimal.valueOf(100), pastDate, futureDate);
        when(savingsGoalRepository.findByIdAndUser(goal.getId(), testUser)).thenReturn(Optional.of(goal));
        when(transactionRepository.findByUserAndDateBetween(testUser, pastDate, today))
            .thenReturn(mockSingleTransaction(BigDecimal.valueOf(150), TransactionType.INCOME, pastDate.plusDays(5)));

        SavingsGoalResponseDto dto = savingsGoalService.getGoalById(goal.getId(), testUser.getEmail());

        assertEquals(BigDecimal.valueOf(150).setScale(2), dto.currentProgress());
        assertEquals(100.0, dto.progressPercentage()); // Capped at 100%
        assertEquals(BigDecimal.valueOf(-50).setScale(2), dto.remainingAmount()); // Remaining can be negative
    }

    @Test
    void getGoalById_checkProgressCalculation_targetDatePassed() {
        LocalDate goalStartDate = today.minusMonths(2);
        LocalDate goalTargetDate = today.minusMonths(1); // Target date in the past
        SavingsGoal goal = createSampleGoal(BigDecimal.valueOf(1000), goalStartDate, goalTargetDate);

        when(savingsGoalRepository.findByIdAndUser(goal.getId(), testUser)).thenReturn(Optional.of(goal));
        // Transactions should only be fetched up to goalTargetDate
        when(transactionRepository.findByUserAndDateBetween(testUser, goalStartDate, goalTargetDate))
            .thenReturn(mockSingleTransaction(BigDecimal.valueOf(300), TransactionType.INCOME, goalStartDate.plusDays(5)));

        SavingsGoalResponseDto dto = savingsGoalService.getGoalById(goal.getId(), testUser.getEmail());

        assertEquals(BigDecimal.valueOf(300).setScale(2), dto.currentProgress());
        assertEquals(30.0, dto.progressPercentage());
        assertEquals(BigDecimal.valueOf(700).setScale(2), dto.remainingAmount());
        verify(transactionRepository).findByUserAndDateBetween(testUser, goalStartDate, goalTargetDate);
    }


    @Test
    void getAllGoals_success() {
        SavingsGoal goal1 = createSampleGoal(BigDecimal.valueOf(500), pastDate, futureDate);
        SavingsGoal goal2 = createSampleGoal(BigDecimal.valueOf(200), today, futureDate.plusMonths(1));
        when(savingsGoalRepository.findByUser(testUser)).thenReturn(Arrays.asList(goal1, goal2));
        // Mock transactions for each goal's progress calculation
        when(transactionRepository.findByUserAndDateBetween(testUser, pastDate, today)).thenReturn(Collections.emptyList());
        when(transactionRepository.findByUserAndDateBetween(testUser, today, today)).thenReturn(Collections.emptyList());


        List<SavingsGoalResponseDto> dtos = savingsGoalService.getAllGoals(testUser.getEmail());
        assertEquals(2, dtos.size());
    }

    @Test
    void updateGoal_success() {
        SavingsGoal existingGoal = createSampleGoal(BigDecimal.valueOf(1000), pastDate, futureDate);
        UpdateSavingsGoalRequestDto updateDto = new UpdateSavingsGoalRequestDto(BigDecimal.valueOf(1200), futureDate.plusMonths(1));

        when(savingsGoalRepository.findByIdAndUser(existingGoal.getId(), testUser)).thenReturn(Optional.of(existingGoal));
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findByUserAndDateBetween(any(), any(), any())).thenReturn(Collections.emptyList());


        SavingsGoalResponseDto responseDto = savingsGoalService.updateGoal(existingGoal.getId(), updateDto, testUser.getEmail());

        assertNotNull(responseDto);
        assertEquals(BigDecimal.valueOf(1200.00).setScale(2), responseDto.targetAmount());
        assertEquals(futureDate.plusMonths(1), responseDto.targetDate());
        verify(savingsGoalRepository).save(any(SavingsGoal.class));
    }

    @Test
    void updateGoal_targetDateNotFuture_throwsException() {
        SavingsGoal existingGoal = createSampleGoal(BigDecimal.valueOf(1000), pastDate, futureDate);
        UpdateSavingsGoalRequestDto updateDto = new UpdateSavingsGoalRequestDto(null, today.minusDays(1)); // Target date in past

        when(savingsGoalRepository.findByIdAndUser(existingGoal.getId(), testUser)).thenReturn(Optional.of(existingGoal));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> savingsGoalService.updateGoal(existingGoal.getId(), updateDto, testUser.getEmail()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Target date must be in the future"));
    }

    @Test
    void updateGoal_targetDateBeforeStartDate_throwsException() {
        SavingsGoal existingGoal = createSampleGoal(BigDecimal.valueOf(1000), today, futureDate); // Starts today
        UpdateSavingsGoalRequestDto updateDto = new UpdateSavingsGoalRequestDto(null, today.minusDays(10)); // Target date before start date

        when(savingsGoalRepository.findByIdAndUser(existingGoal.getId(), testUser)).thenReturn(Optional.of(existingGoal));
         // This will fail the !newTargetDate.isAfter(LocalDate.now()) first in current code
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> savingsGoalService.updateGoal(existingGoal.getId(), updateDto, testUser.getEmail()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        // Depending on which check hits first
        // assertTrue(ex.getReason().contains("Target date cannot be before the goal's start date") || ex.getReason().contains("Target date must be in the future"));
         assertTrue(ex.getReason().contains("Target date must be in the future"));

    }


    @Test
    void deleteGoal_success() {
        SavingsGoal goal = createSampleGoal(BigDecimal.valueOf(100), pastDate, futureDate);
        when(savingsGoalRepository.findByIdAndUser(goal.getId(), testUser)).thenReturn(Optional.of(goal));
        doNothing().when(savingsGoalRepository).delete(goal);

        assertDoesNotThrow(() -> savingsGoalService.deleteGoal(goal.getId(), testUser.getEmail()));
        verify(savingsGoalRepository).delete(goal);
    }

    @Test
    void deleteGoal_notFound() {
        UUID goalId = UUID.randomUUID();
        when(savingsGoalRepository.findByIdAndUser(goalId, testUser)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> savingsGoalService.deleteGoal(goalId, testUser.getEmail()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

}
