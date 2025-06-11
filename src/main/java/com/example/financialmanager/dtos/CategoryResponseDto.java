package com.example.financialmanager.dtos;

// import java.util.UUID; // No longer needed for id or userId
import com.example.financialmanager.entities.TransactionType;

public record CategoryResponseDto(
    String name,
    TransactionType type,
    boolean isCustom
    // UUID id, // Removed
    // String name, // Kept, but re-ordered
    // UUID userId // Removed
) {}
