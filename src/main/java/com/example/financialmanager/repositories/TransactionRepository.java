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
           "AND (:category IS NULL OR LOWER(t.category) LIKE LOWER(CONCAT('%', :category, '%'))) " +
           "AND (:type IS NULL OR t.type = :type)")
    List<Transaction> findTransactionsByFilters(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("category") String category,
        @Param("type") TransactionType type
    );

    // Additional simpler query methods as discussed in the plan
    List<Transaction> findByUser(User user);
    List<Transaction> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    List<Transaction> findByUserAndCategoryContainingIgnoreCase(User user, String category);
    List<Transaction> findByUserAndType(User user, TransactionType type);

    boolean existsByUserAndCategoryIgnoreCase(User user, String category);

    @Query("SELECT new com.example.financialmanager.dtos.CategoryTotalDto(t.category, SUM(t.amount), t.type) " +
           "FROM Transaction t " +
           "WHERE t.user = :user AND t.date >= :startDate AND t.date <= :endDate " +
           "GROUP BY t.category, t.type")
    List<com.example.financialmanager.dtos.CategoryTotalDto> getCategoryTotalsByUserAndDateRange(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
