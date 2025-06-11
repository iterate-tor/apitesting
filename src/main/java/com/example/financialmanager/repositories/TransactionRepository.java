package com.example.financialmanager.repositories;

import com.example.financialmanager.entities.Transaction;
import com.example.financialmanager.entities.TransactionType;
import com.example.financialmanager.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserId(UUID userId);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
           "AND (:startDate IS NULL OR t.date >= :startDate) " +
           "AND (:endDate IS NULL OR t.date <= :endDate) " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " + // Changed category to categoryId
           "AND (:type IS NULL OR t.type = :type) " +
           "ORDER BY t.date DESC, t.id DESC") // Added ORDER BY
    List<Transaction> findTransactionsByFilters(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("categoryId") UUID categoryId, // Changed category to categoryId
        @Param("type") TransactionType type
    );

    // Additional simpler query methods as discussed in the plan
    List<Transaction> findByUser(User user);
    List<Transaction> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    // findByUserAndCategoryContainingIgnoreCase is no longer valid due to Category entity change
    // List<Transaction> findByUserAndCategoryContainingIgnoreCase(User user, String category);
    List<Transaction> findByUserAndCategory(User user, com.example.financialmanager.entities.Category category); // New method if needed
    List<Transaction> findByUserAndType(User user, TransactionType type);

    // boolean existsByUserAndCategoryIgnoreCase(User user, String category); // Old
    boolean existsByCategory(com.example.financialmanager.entities.Category category); // New

    @Query("SELECT new com.example.financialmanager.dtos.CategoryTotalDto(t.category.name, SUM(t.amount), t.type) " + // Use category.name
           "FROM Transaction t " +
           "WHERE t.user = :user AND t.date >= :startDate AND t.date <= :endDate " +
           "GROUP BY t.category.name, t.type") // Use category.name
    List<com.example.financialmanager.dtos.CategoryTotalDto> getCategoryTotalsByUserAndDateRange(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
