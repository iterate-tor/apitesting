package com.example.financialmanager.services;

import com.example.financialmanager.dtos.TransactionRequestDto;
import com.example.financialmanager.dtos.TransactionResponseDto;
import com.example.financialmanager.dtos.UpdateTransactionRequestDto;
import com.example.financialmanager.entities.Category; // Added
import com.example.financialmanager.entities.Transaction;
import com.example.financialmanager.entities.TransactionType;
import com.example.financialmanager.entities.User;
import com.example.financialmanager.repositories.CategoryRepository; // Added
import com.example.financialmanager.repositories.TransactionRepository;
import com.example.financialmanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// import java.time.LocalDate; // Already imported
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository; // Added

    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  UserRepository userRepository,
                                  CategoryRepository categoryRepository) { // Added
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository; // Added
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    private TransactionResponseDto convertToDto(Transaction transaction) {
        return new TransactionResponseDto(
            transaction.getId(),
            transaction.getAmount(),
            transaction.getDate(),
            transaction.getCategory().getName(), // Changed to get category name
            transaction.getDescription(),
            transaction.getType()
            // userId removed as per previous DTO update
        );
    }

    @Override
    @Transactional
    public TransactionResponseDto createTransaction(TransactionRequestDto requestDto, String userEmail) {
        User user = getUserByEmail(userEmail);

        Category category = categoryRepository.findById(requestDto.categoryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category ID: Category not found"));

        // Check category usability: if custom, must belong to the user. Default categories are fine.
        if (category.isCustom() && (category.getUser() == null || !category.getUser().getId().equals(user.getId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified custom category is not accessible by this user.");
        }

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category); // Set Category entity
        transaction.setAmount(requestDto.amount());
        transaction.setDate(requestDto.date());
        transaction.setDescription(requestDto.description());
        transaction.setType(requestDto.type());

        Transaction savedTransaction = transactionRepository.save(transaction);
        return convertToDto(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactions(String userEmail, LocalDate startDate, LocalDate endDate, UUID categoryId, TransactionType type) {
        User user = getUserByEmail(userEmail);
        // Parameter 'category' changed to 'categoryId'
        List<Transaction> transactions = transactionRepository.findTransactionsByFilters(user, startDate, endDate, categoryId, type);
        return transactions.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionById(UUID transactionId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this transaction");
        }
        return convertToDto(transaction); // DTO conversion is updated
    }

    @Override
    @Transactional
    public TransactionResponseDto updateTransaction(UUID transactionId, UpdateTransactionRequestDto requestDto, String userEmail) {
        User user = getUserByEmail(userEmail);
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this transaction");
        }

        // Spec: "Users can modify any transaction field except the date field."
        if (requestDto.amount() != null) {
            // Validation for positivity is on DTO, but can be re-checked if desired
            transaction.setAmount(requestDto.amount());
        }
        if (requestDto.categoryId() != null) {
            Category newCategory = categoryRepository.findById(requestDto.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category ID: New category not found"));
            // Check category usability for the new category
            if (newCategory.isCustom() && (newCategory.getUser() == null || !newCategory.getUser().getId().equals(user.getId()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified new custom category is not accessible by this user.");
            }
            transaction.setCategory(newCategory);
        }
        if (requestDto.description() != null) {
            transaction.setDescription(requestDto.description());
        }
        if (requestDto.type() != null) {
            transaction.setType(requestDto.type());
        }
        // Date is not updated.

        Transaction updatedTransaction = transactionRepository.save(transaction);
        return convertToDto(updatedTransaction);
    }

    @Override
    @Transactional
    public void deleteTransaction(UUID transactionId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this transaction");
        }
        transactionRepository.delete(transaction);
    }
}
