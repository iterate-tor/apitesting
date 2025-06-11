package com.example.financialmanager.repositories;

import com.example.financialmanager.entities.Category;
import com.example.financialmanager.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByUserAndNameIgnoreCase(User user, String name);

    List<Category> findByUser(User user);

    boolean existsByUserAndNameIgnoreCase(User user, String name);
}
