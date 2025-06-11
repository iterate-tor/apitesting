package com.example.financialmanager.services;

import com.example.financialmanager.dtos.TransactionRequestDto;
import com.example.financialmanager.dtos.TransactionResponseDto;
import com.example.financialmanager.dtos.UpdateTransactionRequestDto;
import com.example.financialmanager.entities.TransactionType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionService {

    TransactionResponseDto createTransaction(TransactionRequestDto requestDto, String userEmail);

    List<TransactionResponseDto> getTransactions(
        String userEmail,
        LocalDate startDate,
        LocalDate endDate,
        UUID categoryId, // Changed from String category
        TransactionType type
    );

    TransactionResponseDto getTransactionById(UUID transactionId, String userEmail);

    TransactionResponseDto updateTransaction(
        UUID transactionId,
        UpdateTransactionRequestDto requestDto, // Changed from TransactionRequestDto
        String userEmail
    );

    void deleteTransaction(UUID transactionId, String userEmail);
}
