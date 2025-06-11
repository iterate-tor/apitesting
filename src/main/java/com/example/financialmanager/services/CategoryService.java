package com.example.financialmanager.services;

import com.example.financialmanager.dtos.CategoryRequestDto;
import com.example.financialmanager.dtos.CategoryResponseDto;

import java.util.List;
// UUID import might be needed if we decide to use it for delete by ID later,
// but current spec is delete by name.

public interface CategoryService {

    CategoryResponseDto createCategory(CategoryRequestDto requestDto, String userEmail);

    List<CategoryResponseDto> getCategories(String userEmail);

    void deleteCategory(String categoryName, String userEmail);
}
