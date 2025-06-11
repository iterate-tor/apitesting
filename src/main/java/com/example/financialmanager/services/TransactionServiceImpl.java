package com.example.financialmanager.services;

import com.example.financialmanager.dtos.TransactionRequestDto;
import com.example.financialmanager.dtos.TransactionResponseDto;
import com.example.financialmanager.dtos.UpdateTransactionRequestDto;
import com.example.financialmanager.entities.Transaction;
import com.example.financialmanager.entities.TransactionType;
import com.example.financialmanager.entities.User;
import com.example.financialmanager.repositories.TransactionRepository;
import com.example.financialmanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // Helper method to fetch user
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    // Helper method to convert Entity to DTO
    private TransactionResponseDto convertToDto(Transaction transaction) {
        return new TransactionResponseDto(
            transaction.getId(),
            transaction.getAmount(),
            transaction.getDate(),
            transaction.getCategory(),
            transaction.getDescription(),
            transaction.getType(),
            transaction.getUser().getId()
        );
    }

    // Note: convertToEntity from TransactionRequestDto is simple enough to be inline or part of create.
    // For update, it's different.

    @Override
    @Transactional
    public TransactionResponseDto createTransaction(TransactionRequestDto requestDto, String userEmail) {
        User user = getUserByEmail(userEmail);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(requestDto.amount()); // Amount is already positive from DTO validation
        transaction.setDate(requestDto.date());
        transaction.setCategory(requestDto.category());
        transaction.setDescription(requestDto.description());
        transaction.setType(requestDto.type()); // Type is explicitly set from DTO

        Transaction savedTransaction = transactionRepository.save(transaction);
        return convertToDto(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactions(String userEmail, LocalDate startDate, LocalDate endDate, String category, TransactionType type) {
        User user = getUserByEmail(userEmail);
        List<Transaction> transactions = transactionRepository.findTransactionsByFilters(user, startDate, endDate, category, type);
        return transactions.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionById(UUID transactionId, String userEmail) {
        User user = getUserByEmail(userEmail); // Ensure user exists
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this transaction");
        }
        return convertToDto(transaction);
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

        // Spec: "Update only amount and/or description"
        if (requestDto.amount() != null) {
            // Amount must be positive. Validation is on DTO, but good to be defensive.
            if (requestDto.amount().signum() <= 0) {
                 throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
            }
            transaction.setAmount(requestDto.amount());
        }
        if (requestDto.description() != null) {
            transaction.setDescription(requestDto.description());
        }
        // Note: Category, Date, and Type are NOT updated as per current spec for this method.

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
