package com.example.financialmanager.controllers;

import com.example.financialmanager.dtos.TransactionRequestDto;
import com.example.financialmanager.dtos.TransactionResponseDto;
import com.example.financialmanager.dtos.UpdateTransactionRequestDto;
import com.example.financialmanager.dtos.MessageResponseDto; // Added for delete response
import com.example.financialmanager.entities.TransactionType;
import com.example.financialmanager.services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map; // Added for GET /api/transactions response wrapper
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping
    public ResponseEntity<TransactionResponseDto> createTransaction(@Valid @RequestBody TransactionRequestDto transactionDto) {
        String userEmail = getCurrentUserEmail();
        TransactionResponseDto createdTransaction = transactionService.createTransaction(transactionDto, userEmail);
        return new ResponseEntity<>(createdTransaction, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Map<String, List<TransactionResponseDto>>> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID categoryId, // Changed from String category
            @RequestParam(required = false) TransactionType type) {
        String userEmail = getCurrentUserEmail();
        List<TransactionResponseDto> transactions = transactionService.getTransactions(userEmail, startDate, endDate, categoryId, type);
        Map<String, List<TransactionResponseDto>> response = Map.of("transactions", transactions); // Wrapped response
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable UUID id) {
        String userEmail = getCurrentUserEmail();
        TransactionResponseDto transaction = transactionService.getTransactionById(id, userEmail);
        return ResponseEntity.ok(transaction);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> updateTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequestDto transactionDto) {
        String userEmail = getCurrentUserEmail();
        TransactionResponseDto updatedTransaction = transactionService.updateTransaction(id, transactionDto, userEmail);
        return ResponseEntity.ok(updatedTransaction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponseDto> deleteTransaction(@PathVariable UUID id) { // Return type changed
        String userEmail = getCurrentUserEmail();
        transactionService.deleteTransaction(id, userEmail);
        return ResponseEntity.ok(new MessageResponseDto("Transaction deleted successfully")); // New response body
    }
}
