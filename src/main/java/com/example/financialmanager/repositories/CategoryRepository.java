package com.example.financialmanager.repositories;

import com.example.financialmanager.entities.Category;
import com.example.financialmanager.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.financialmanager.entities.TransactionType; // Needed for new method

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // Optional<Category> findByUserAndNameIgnoreCase(User user, String name); // Replaced or made more specific
    List<Category> findByUser(User user); // Keep for general user category fetching if needed, or remove if findByUserAndIsCustomTrue is sufficient

    // boolean existsByUserAndNameIgnoreCase(User user, String name); // Replaced

    // New methods for refactored service:
    List<Category> findByUserAndIsCustomTrue(User user, boolean isCustom);

    Optional<Category> findByUserAndNameIgnoreCaseAndIsCustomTrue(User user, String name, boolean isCustom);

    boolean existsByUserAndNameIgnoreCaseAndTypeAndIsCustomTrue(User user, String name, TransactionType type, boolean isCustom);
}
