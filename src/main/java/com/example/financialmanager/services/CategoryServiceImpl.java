package com.example.financialmanager.services;

import com.example.financialmanager.dtos.CategoryRequestDto;
import com.example.financialmanager.dtos.CategoryResponseDto;
import com.example.financialmanager.entities.Category;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    private static final List<String> DEFAULT_CATEGORY_NAMES = Arrays.asList(
        "Salary", "Groceries", "Utilities", "Rent/Mortgage", "Transportation", "Entertainment", "Healthcare", "Education", "Dining Out", "Savings"
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

    private CategoryResponseDto convertToDto(Category category) {
        return new CategoryResponseDto(
            category.getId(),
            category.getName(),
            category.getUser().getId()
        );
    }

    private CategoryResponseDto convertDefaultToDto(String name) {
        // Default categories don't have a user-specific ID or a specific user ID from the DB
        return new CategoryResponseDto(null, name, null);
    }

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto requestDto, String userEmail) {
        User user = getUserByEmail(userEmail);
        String categoryName = requestDto.name().trim();

        if (categoryRepository.existsByUserAndNameIgnoreCase(user, categoryName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category with this name '" + categoryName + "' already exists for this user.");
        }

        // As per self-correction, allowing user category names that match default ones.
        // The uniqueness is per user.

        Category category = new Category(user, categoryName);
        Category savedCategory = categoryRepository.save(category);
        return convertToDto(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getCategories(String userEmail) {
        User user = getUserByEmail(userEmail);

        List<CategoryResponseDto> userCategories = categoryRepository.findByUser(user)
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

        List<CategoryResponseDto> defaultCategories = DEFAULT_CATEGORY_NAMES.stream()
            .map(this::convertDefaultToDto)
            .collect(Collectors.toList());

        // Combine and return. Client can handle any display logic for duplicates if user created one with same name as default.
        // Or, to provide a distinct list by name:
        List<CategoryResponseDto> combined = Stream.concat(userCategories.stream(), defaultCategories.stream())
            .collect(Collectors.toList());

        // A more sophisticated approach to avoid duplicates by name if user created one matching default:
        // Create a Set of names from userCategories, then filter defaultCategories
        // For now, simple concatenation is fine as per "Returns default + user-created categories"
        return combined;
    }

    @Override
    @Transactional
    public void deleteCategory(String categoryName, String userEmail) {
        User user = getUserByEmail(userEmail);
        String trimmedCategoryName = categoryName.trim();

        // Check if it's a default category - these cannot be deleted by this mechanism
        if (DEFAULT_CATEGORY_NAMES.stream().anyMatch(defaultName -> defaultName.equalsIgnoreCase(trimmedCategoryName))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default categories cannot be deleted.");
        }

        Category category = categoryRepository.findByUserAndNameIgnoreCase(user, trimmedCategoryName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category '" + trimmedCategoryName + "' not found for this user."));

        if (transactionRepository.existsByUserAndCategoryIgnoreCase(user, trimmedCategoryName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category '" + trimmedCategoryName + "' is in use by transactions and cannot be deleted.");
        }

        categoryRepository.delete(category);
    }
}
