package com.example.financialmanager.controllers;

import com.example.financialmanager.dtos.CategoryRequestDto;
import com.example.financialmanager.dtos.CategoryResponseDto;
import com.example.financialmanager.dtos.MessageResponseDto; // Added for delete response
import com.example.financialmanager.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map; // Added for GET /api/categories response wrapper

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping
    public ResponseEntity<Map<String, List<CategoryResponseDto>>> getCategories() { // Response wrapped in Map
        String userEmail = getCurrentUserEmail();
        List<CategoryResponseDto> categories = categoryService.getCategories(userEmail);
        Map<String, List<CategoryResponseDto>> response = Map.of("categories", categories);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDto> createCategory(@Valid @RequestBody CategoryRequestDto categoryDto) { // Accepts new DTO
        String userEmail = getCurrentUserEmail();
        CategoryResponseDto createdCategory = categoryService.createCategory(categoryDto, userEmail); // Returns new DTO
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<MessageResponseDto> deleteCategory(@PathVariable String name) { // Returns MessageResponseDto
        String userEmail = getCurrentUserEmail();
        categoryService.deleteCategory(name, userEmail);
        return ResponseEntity.ok(new MessageResponseDto("Category deleted successfully")); // New response body
    }
}
