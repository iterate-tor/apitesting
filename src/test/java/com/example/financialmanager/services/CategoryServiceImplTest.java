package com.example.financialmanager.services;

import com.example.financialmanager.dtos.CategoryRequestDto;
import com.example.financialmanager.dtos.CategoryResponseDto;
import com.example.financialmanager.entities.Category;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private User testUser;
    private UUID userId;

    // Accessing DEFAULT_CATEGORIES from CategoryServiceImpl for tests is tricky as it's private.
    // Replicating a small sample or making it package-private/test-visible in Impl would be alternatives.
    // For this test, I'll use knowledge of some default names.
    private static final List<CategoryServiceImpl.DefaultCategory> TEST_DEFAULT_CATEGORIES = Arrays.asList(
        new CategoryServiceImpl.DefaultCategory("Salary", TransactionType.INCOME),
        new CategoryServiceImpl.DefaultCategory("Groceries", TransactionType.EXPENSE)
    );


    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User("user@example.com", "password", "Test User", "123");
        testUser.setId(userId);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
    }

    @Test
    void getCategories_returnsDefaultAndCustom() {
        Category customCat1 = new Category(testUser, "Freelancing", TransactionType.INCOME, true);
        customCat1.setId(UUID.randomUUID());
        Category customCat2 = new Category(testUser, "Hobbies", TransactionType.EXPENSE, true);
        customCat2.setId(UUID.randomUUID());

        when(categoryRepository.findByUserAndIsCustomTrue(testUser, true)).thenReturn(Arrays.asList(customCat1, customCat2));

        List<CategoryResponseDto> responseDtos = categoryService.getCategories(testUser.getEmail());

        assertNotNull(responseDtos);
        // Expected size = default categories in CategoryServiceImpl + 2 custom ones
        // Using the known TEST_DEFAULT_CATEGORIES size for this assertion.
        // A more robust test might directly access or mock the DEFAULT_CATEGORIES list from the service.
        long expectedDefaultCount = CategoryServiceImpl.DEFAULT_CATEGORIES.size(); // Actual default count
        assertEquals(expectedDefaultCount + 2, responseDtos.size());


        assertTrue(responseDtos.stream().anyMatch(dto -> dto.name().equals("Freelancing") && dto.type() == TransactionType.INCOME && dto.isCustom()));
        assertTrue(responseDtos.stream().anyMatch(dto -> dto.name().equals("Hobbies") && dto.type() == TransactionType.EXPENSE && dto.isCustom()));
        assertTrue(responseDtos.stream().anyMatch(dto -> dto.name().equals("Salary") && dto.type() == TransactionType.INCOME && !dto.isCustom()));
    }

    @Test
    void createCategory_success() {
        CategoryRequestDto requestDto = new CategoryRequestDto("My Custom Income", TransactionType.INCOME);
        Category savedCategory = new Category(testUser, "My Custom Income", TransactionType.INCOME, true);
        savedCategory.setId(UUID.randomUUID());

        when(categoryRepository.existsByUserAndNameIgnoreCaseAndTypeAndIsCustomTrue(testUser, requestDto.name(), requestDto.type(), true)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponseDto responseDto = categoryService.createCategory(requestDto, testUser.getEmail());

        assertNotNull(responseDto);
        assertEquals("My Custom Income", responseDto.name());
        assertEquals(TransactionType.INCOME, responseDto.type());
        assertTrue(responseDto.isCustom());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_nameConflictsWithDefault() {
        // Assuming "Salary" is a default category name (case-insensitive check)
        CategoryRequestDto requestDto = new CategoryRequestDto("salary", TransactionType.INCOME);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoryService.createCategory(requestDto, testUser.getEmail()));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("conflicts with a default category"));
    }

    @Test
    void createCategory_customCategoryAlreadyExists() {
        CategoryRequestDto requestDto = new CategoryRequestDto("My Custom Expense", TransactionType.EXPENSE);
        when(categoryRepository.existsByUserAndNameIgnoreCaseAndTypeAndIsCustomTrue(testUser, requestDto.name(), requestDto.type(), true)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoryService.createCategory(requestDto, testUser.getEmail()));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Custom category with name"));
    }

    @Test
    void deleteCategory_success() {
        String categoryNameToDelete = "Old Hobby";
        Category customCategory = new Category(testUser, categoryNameToDelete, TransactionType.EXPENSE, true);
        customCategory.setId(UUID.randomUUID());

        when(categoryRepository.findByUserAndNameIgnoreCaseAndIsCustomTrue(testUser, categoryNameToDelete, true)).thenReturn(Optional.of(customCategory));
        when(transactionRepository.existsByCategory(customCategory)).thenReturn(false); // Not in use
        doNothing().when(categoryRepository).delete(customCategory);

        assertDoesNotThrow(() -> categoryService.deleteCategory(categoryNameToDelete, testUser.getEmail()));
        verify(categoryRepository).delete(customCategory);
    }

    @Test
    void deleteCategory_attemptToDeleteDefault() {
        // Assuming "Groceries" is a default category.
        String defaultCategoryName = "Groceries";

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoryService.deleteCategory(defaultCategoryName, testUser.getEmail()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Default categories cannot be deleted"));
    }

    @Test
    void deleteCategory_customCategoryNotFound() {
        String categoryNameToDelete = "NonExistent";
        when(categoryRepository.findByUserAndNameIgnoreCaseAndIsCustomTrue(testUser, categoryNameToDelete, true)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoryService.deleteCategory(categoryNameToDelete, testUser.getEmail()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Custom category '" + categoryNameToDelete + "' not found"));
    }

    @Test
    void deleteCategory_customCategoryInUse() {
        String categoryNameToDelete = "Active Hobby";
        Category customCategory = new Category(testUser, categoryNameToDelete, TransactionType.EXPENSE, true);
        customCategory.setId(UUID.randomUUID());

        when(categoryRepository.findByUserAndNameIgnoreCaseAndIsCustomTrue(testUser, categoryNameToDelete, true)).thenReturn(Optional.of(customCategory));
        when(transactionRepository.existsByCategory(customCategory)).thenReturn(true); // Category is in use

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoryService.deleteCategory(categoryNameToDelete, testUser.getEmail()));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("is in use by transactions and cannot be deleted"));
    }
}
