package com.example.financialmanager.services;

import com.example.financialmanager.dtos.TransactionRequestDto;
import com.example.financialmanager.dtos.TransactionResponseDto;
import com.example.financialmanager.dtos.UpdateTransactionRequestDto;
import com.example.financialmanager.entities.Category;
import com.example.financialmanager.entities.Transaction;
import com.example.financialmanager.entities.TransactionType;
import com.example.financialmanager.entities.User;
import com.example.financialmanager.repositories.CategoryRepository;
import com.example.financialmanager.repositories.TransactionRepository;
import com.example.financialmanager.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User testUser;
    private UUID userId;
    private Category testCategory;
    private UUID categoryId;
    private Category otherUserCategory;


    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User("user@example.com", "password", "Test User", "123");
        testUser.setId(userId);

        categoryId = UUID.randomUUID();
        testCategory = new Category(testUser, "Groceries", TransactionType.EXPENSE, true); // Custom category owned by testUser
        testCategory.setId(categoryId);

        User anotherUser = new User("other@example.com", "password", "Other User", "321");
        anotherUser.setId(UUID.randomUUID());
        otherUserCategory = new Category(anotherUser, "Other Groceries", TransactionType.EXPENSE, true);
        otherUserCategory.setId(UUID.randomUUID());


        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
    }

    // --- createTransaction Tests ---
    @Test
    void createTransaction_success() {
        TransactionRequestDto requestDto = new TransactionRequestDto(BigDecimal.TEN, LocalDate.now(), categoryId, "Milk", TransactionType.EXPENSE);
        Transaction savedTransaction = new Transaction(testUser, BigDecimal.TEN, LocalDate.now(), testCategory, "Milk", TransactionType.EXPENSE);
        savedTransaction.setId(UUID.randomUUID());

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponseDto responseDto = transactionService.createTransaction(requestDto, testUser.getEmail());

        assertNotNull(responseDto);
        assertEquals(savedTransaction.getId(), responseDto.id());
        assertEquals(testCategory.getName(), responseDto.categoryName());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_categoryNotFound() {
        TransactionRequestDto requestDto = new TransactionRequestDto(BigDecimal.TEN, LocalDate.now(), UUID.randomUUID(), "Milk", TransactionType.EXPENSE);
        when(categoryRepository.findById(requestDto.categoryId())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionService.createTransaction(requestDto, testUser.getEmail()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid category ID: Category not found"));
    }

    @Test
    void createTransaction_customCategoryNotOwnedByUser() {
        TransactionRequestDto requestDto = new TransactionRequestDto(BigDecimal.TEN, LocalDate.now(), otherUserCategory.getId(), "Milk", TransactionType.EXPENSE);
        when(categoryRepository.findById(otherUserCategory.getId())).thenReturn(Optional.of(otherUserCategory));
        // testUser is attempting to use otherUserCategory

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionService.createTransaction(requestDto, testUser.getEmail()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Specified custom category is not accessible by this user"));
    }

    @Test
    void createTransaction_defaultCategory_success() {
        UUID defaultCatId = UUID.randomUUID();
        Category defaultCategory = new Category(null, "Salary", TransactionType.INCOME, false); // Default category
        defaultCategory.setId(defaultCatId);

        TransactionRequestDto requestDto = new TransactionRequestDto(BigDecimal.valueOf(100), LocalDate.now(), defaultCatId, "Paycheck", TransactionType.INCOME);
        Transaction savedTransaction = new Transaction(testUser, BigDecimal.valueOf(100), LocalDate.now(), defaultCategory, "Paycheck", TransactionType.INCOME);
        savedTransaction.setId(UUID.randomUUID());

        when(categoryRepository.findById(defaultCatId)).thenReturn(Optional.of(defaultCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponseDto responseDto = transactionService.createTransaction(requestDto, testUser.getEmail());

        assertNotNull(responseDto);
        assertEquals(defaultCategory.getName(), responseDto.categoryName());
        verify(transactionRepository).save(any(Transaction.class));
    }


    // --- getTransactions Tests ---
    @Test
    void getTransactions_success() {
        Transaction transaction = new Transaction(testUser, BigDecimal.TEN, LocalDate.now(), testCategory, "Milk", TransactionType.EXPENSE);
        transaction.setId(UUID.randomUUID());
        List<Transaction> transactions = Collections.singletonList(transaction);

        when(transactionRepository.findTransactionsByFilters(eq(testUser), any(), any(), any(), any())).thenReturn(transactions);

        List<TransactionResponseDto> responseDtos = transactionService.getTransactions(testUser.getEmail(), null, null, null, null);

        assertFalse(responseDtos.isEmpty());
        assertEquals(1, responseDtos.size());
        assertEquals(transaction.getId(), responseDtos.get(0).id());
    }

    // --- getTransactionById Tests ---
    @Test
    void getTransactionById_success() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(testUser, BigDecimal.TEN, LocalDate.now(), testCategory, "Milk", TransactionType.EXPENSE);
        transaction.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        TransactionResponseDto responseDto = transactionService.getTransactionById(transactionId, testUser.getEmail());

        assertNotNull(responseDto);
        assertEquals(transactionId, responseDto.id());
    }

    @Test
    void getTransactionById_notFound() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionService.getTransactionById(transactionId, testUser.getEmail()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getTransactionById_notOwned() {
        UUID transactionId = UUID.randomUUID();
        User anotherUser = new User();
        anotherUser.setId(UUID.randomUUID()); // Different user ID
        Transaction transaction = new Transaction(anotherUser, BigDecimal.TEN, LocalDate.now(), testCategory, "Milk", TransactionType.EXPENSE);
        transaction.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionService.getTransactionById(transactionId, testUser.getEmail()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // --- updateTransaction Tests ---
    @Test
    void updateTransaction_success() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = new Transaction(testUser, BigDecimal.TEN, LocalDate.now().minusDays(1), testCategory, "Old Desc", TransactionType.EXPENSE);
        existingTransaction.setId(transactionId);

        UpdateTransactionRequestDto requestDto = new UpdateTransactionRequestDto(BigDecimal.valueOf(20), null, "New Desc", TransactionType.INCOME);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existingTransaction));
        // No categoryId in requestDto, so categoryRepository.findById for new category won't be called
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));


        TransactionResponseDto responseDto = transactionService.updateTransaction(transactionId, requestDto, testUser.getEmail());

        assertNotNull(responseDto);
        assertEquals(BigDecimal.valueOf(20.00).setScale(2), responseDto.amount().setScale(2)); // amount is updated
        assertEquals("New Desc", responseDto.description());
        assertEquals(TransactionType.INCOME, responseDto.type());
        assertEquals(testCategory.getName(), responseDto.categoryName()); // Category remains same
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_updateCategory_success() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = new Transaction(testUser, BigDecimal.TEN, LocalDate.now().minusDays(1), testCategory, "Old Desc", TransactionType.EXPENSE);
        existingTransaction.setId(transactionId);

        UUID newCatId = UUID.randomUUID();
        Category newCategory = new Category(testUser, "Salary", TransactionType.INCOME, true);
        newCategory.setId(newCatId);

        UpdateTransactionRequestDto requestDto = new UpdateTransactionRequestDto(null, newCatId, null, null);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existingTransaction));
        when(categoryRepository.findById(newCatId)).thenReturn(Optional.of(newCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponseDto responseDto = transactionService.updateTransaction(transactionId, requestDto, testUser.getEmail());

        assertNotNull(responseDto);
        assertEquals(newCategory.getName(), responseDto.categoryName());
        verify(transactionRepository).save(any(Transaction.class));
    }


    @Test
    void updateTransaction_newCategoryNotFound() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = new Transaction(testUser, BigDecimal.TEN, LocalDate.now(), testCategory, "Desc", TransactionType.EXPENSE);
        existingTransaction.setId(transactionId);
        UUID newCategoryId = UUID.randomUUID();
        UpdateTransactionRequestDto requestDto = new UpdateTransactionRequestDto(null, newCategoryId, null, null);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existingTransaction));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionService.updateTransaction(transactionId, requestDto, testUser.getEmail()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid category ID: New category not found"));
    }

    @Test
    void updateTransaction_newCustomCategoryNotOwned() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = new Transaction(testUser, BigDecimal.TEN, LocalDate.now(), testCategory, "Desc", TransactionType.EXPENSE);
        existingTransaction.setId(transactionId);

        UpdateTransactionRequestDto requestDto = new UpdateTransactionRequestDto(null, otherUserCategory.getId(), null, null);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existingTransaction));
        when(categoryRepository.findById(otherUserCategory.getId())).thenReturn(Optional.of(otherUserCategory));


        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionService.updateTransaction(transactionId, requestDto, testUser.getEmail()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Specified new custom category is not accessible by this user."));
    }


    // --- deleteTransaction Tests ---
    @Test
    void deleteTransaction_success() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(testUser, BigDecimal.TEN, LocalDate.now(), testCategory, "Milk", TransactionType.EXPENSE);
        transaction.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        doNothing().when(transactionRepository).delete(transaction);

        assertDoesNotThrow(() -> transactionService.deleteTransaction(transactionId, testUser.getEmail()));
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void deleteTransaction_notFound() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionService.deleteTransaction(transactionId, testUser.getEmail()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(transactionRepository, never()).delete(any());
    }
}
