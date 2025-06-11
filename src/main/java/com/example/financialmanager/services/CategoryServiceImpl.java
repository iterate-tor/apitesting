package com.example.financialmanager.services;

import com.example.financialmanager.dtos.CategoryRequestDto;
import com.example.financialmanager.dtos.CategoryResponseDto;
import com.example.financialmanager.entities.Category;
import com.example.financialmanager.entities.TransactionType; // Added
import com.example.financialmanager.entities.User;
import com.example.financialmanager.repositories.CategoryRepository;
import com.example.financialmanager.repositories.TransactionRepository;
import com.example.financialmanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList; // Added
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
// import java.util.stream.Stream; // Not strictly needed for current concatenation logic

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    // Define DefaultCategory record and the list of default categories
    private record DefaultCategory(String name, TransactionType type) {}

    private static final List<DefaultCategory> DEFAULT_CATEGORIES = Arrays.asList(
        new DefaultCategory("Salary", TransactionType.INCOME),
        new DefaultCategory("Freelance Income", TransactionType.INCOME),
        new DefaultCategory("Investment Income", TransactionType.INCOME),
        new DefaultCategory("Gifts Received", TransactionType.INCOME),
        new DefaultCategory("Other Income", TransactionType.INCOME),
        new DefaultCategory("Groceries", TransactionType.EXPENSE),
        new DefaultCategory("Rent/Mortgage", TransactionType.EXPENSE),
        new DefaultCategory("Utilities", TransactionType.EXPENSE), // (Electricity, Water, Gas, Internet)
        new DefaultCategory("Transportation", TransactionType.EXPENSE), // (Fuel, Public Transit, Car Maintenance)
        new DefaultCategory("Dining Out", TransactionType.EXPENSE),
        new DefaultCategory("Entertainment", TransactionType.EXPENSE), // (Movies, Concerts, Hobbies)
        new DefaultCategory("Healthcare", TransactionType.EXPENSE), // (Medication, Doctor Visits)
        new DefaultCategory("Education", TransactionType.EXPENSE),
        new DefaultCategory("Shopping", TransactionType.EXPENSE), // (Clothing, Electronics, etc.)
        new DefaultCategory("Travel", TransactionType.EXPENSE),
        new DefaultCategory("Insurance", TransactionType.EXPENSE),
        new DefaultCategory("Childcare", TransactionType.EXPENSE),
        new DefaultCategory("Personal Care", TransactionType.EXPENSE), // (Haircuts, Toiletries)
        new DefaultCategory("Gifts Given", TransactionType.EXPENSE),
        new DefaultCategory("Charity/Donations", TransactionType.EXPENSE),
        new DefaultCategory("Taxes", TransactionType.EXPENSE),
        new DefaultCategory("Other Expense", TransactionType.EXPENSE)
    );


    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository, UserRepository userRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    // Updated convertToDto for the new CategoryResponseDto structure
    private CategoryResponseDto convertToDto(Category category) {
        return new CategoryResponseDto(
            category.getName(),
            category.getType(),
            category.isCustom()
        );
    }

    // Helper for default categories
    private CategoryResponseDto convertDefaultToDto(DefaultCategory defaultCategory) {
        return new CategoryResponseDto(defaultCategory.name(), defaultCategory.type(), false);
    }

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto requestDto, String userEmail) {
        User user = getUserByEmail(userEmail);
        String categoryName = requestDto.name().trim();
        TransactionType categoryType = requestDto.type();

        // Check for conflict with default category names (case-insensitive)
        if (DEFAULT_CATEGORIES.stream().anyMatch(defCat -> defCat.name().equalsIgnoreCase(categoryName))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name '" + categoryName + "' conflicts with a default category. Please choose a different name.");
        }

        // Check if a custom category with the same name and type already exists for this user
        if (categoryRepository.existsByUserAndNameIgnoreCaseAndTypeAndIsCustomTrue(user, categoryName, categoryType, true)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Custom category with name '" + categoryName + "' and type '" + categoryType + "' already exists.");
        }

        Category category = new Category(user, categoryName, categoryType, true); // isCustom is true
        Category savedCategory = categoryRepository.save(category);
        return convertToDto(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getCategories(String userEmail) {
        User user = getUserByEmail(userEmail);

        // Fetch user-specific (custom) categories
        List<CategoryResponseDto> userCategories = categoryRepository.findByUserAndIsCustomTrue(user, true)
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

        // Generate DTOs for default categories
        List<CategoryResponseDto> defaultCategoryDtos = DEFAULT_CATEGORIES.stream()
            .map(this::convertDefaultToDto)
            .collect(Collectors.toList());

        // Combine lists
        List<CategoryResponseDto> combinedList = new ArrayList<>(defaultCategoryDtos);
        combinedList.addAll(userCategories);

        return combinedList;
    }

    @Override
    @Transactional
    public void deleteCategory(String categoryName, String userEmail) {
        User user = getUserByEmail(userEmail);
        String trimmedCategoryName = categoryName.trim();

        // Check if it's a default category - these cannot be deleted
        if (DEFAULT_CATEGORIES.stream().anyMatch(defaultCat -> defaultCat.name().equalsIgnoreCase(trimmedCategoryName))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Default categories cannot be deleted.");
        }

        // Find the custom category for this user
        Category category = categoryRepository.findByUserAndNameIgnoreCaseAndIsCustomTrue(user, trimmedCategoryName, true)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom category '" + trimmedCategoryName + "' not found for this user."));

        // Check if the category is used in any transactions
        if (transactionRepository.existsByCategory(category)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category '" + trimmedCategoryName + "' is in use by transactions and cannot be deleted.");
        }

        categoryRepository.delete(category);
    }
}
