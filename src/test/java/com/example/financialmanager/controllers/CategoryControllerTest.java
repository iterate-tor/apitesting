package com.example.financialmanager.controllers;

import com.example.financialmanager.dtos.CategoryRequestDto;
import com.example.financialmanager.dtos.CategoryResponseDto;
import com.example.financialmanager.dtos.MessageResponseDto;
import com.example.financialmanager.entities.TransactionType;
import com.example.financialmanager.services.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@WithMockUser(username = "user@example.com")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    private String userEmail = "user@example.com";
    private CategoryResponseDto categoryResponseDto1;
    private CategoryResponseDto categoryResponseDto2;

    @BeforeEach
    void setUp() {
        categoryResponseDto1 = new CategoryResponseDto("Salary", TransactionType.INCOME, false);
        categoryResponseDto2 = new CategoryResponseDto("Groceries", TransactionType.EXPENSE, true);
    }

    @Test
    void getCategories_success_returns200AndWrappedResponse() throws Exception {
        List<CategoryResponseDto> dtoList = Arrays.asList(categoryResponseDto1, categoryResponseDto2);
        when(categoryService.getCategories(eq(userEmail))).thenReturn(dtoList);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories.length()").value(2))
                .andExpect(jsonPath("$.categories[0].name").value("Salary"))
                .andExpect(jsonPath("$.categories[1].name").value("Groceries"));
    }

    @Test
    void createCategory_validRequest_returns201() throws Exception {
        CategoryRequestDto requestDto = new CategoryRequestDto("Hobby", TransactionType.EXPENSE);
        CategoryResponseDto createdDto = new CategoryResponseDto("Hobby", TransactionType.EXPENSE, true);
        when(categoryService.createCategory(any(CategoryRequestDto.class), eq(userEmail))).thenReturn(createdDto);

        mockMvc.perform(post("/api/categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Hobby"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.isCustom").value(true));
    }

    @Test
    void createCategory_invalidRequest_blankName_returns400() throws Exception {
        CategoryRequestDto invalidDto = new CategoryRequestDto("", TransactionType.EXPENSE);
        // DTO validation handled by Spring before service is called
        mockMvc.perform(post("/api/categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").value("must not be blank"));
    }

    @Test
    void createCategory_invalidRequest_nullType_returns400() throws Exception {
        // Constructing JSON manually as record might not allow null for @NotNull field directly
        String jsonRequest = "{\"name\":\"Valid Name\", \"type\":null}";
        mockMvc.perform(post("/api/categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.type").value("must not be null"));
    }


    @Test
    void createCategory_serviceThrowsConflict_returns409() throws Exception {
        CategoryRequestDto requestDto = new CategoryRequestDto("Salary", TransactionType.INCOME); // Name might conflict
        when(categoryService.createCategory(any(CategoryRequestDto.class), eq(userEmail)))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Category name conflicts with a default category."));

        mockMvc.perform(post("/api/categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category name conflicts with a default category."));
    }

    @Test
    void deleteCategory_success_returns200WithMessage() throws Exception {
        String categoryName = "OldHobby";
        doNothing().when(categoryService).deleteCategory(eq(categoryName), eq(userEmail));

        mockMvc.perform(delete("/api/categories/{name}", categoryName).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
    }

    @Test
    void deleteCategory_notFound_returns404() throws Exception {
        String categoryName = "NotFound";
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom category not found."))
            .when(categoryService).deleteCategory(eq(categoryName), eq(userEmail));

        mockMvc.perform(delete("/api/categories/{name}", categoryName).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Custom category not found."));
    }

    @Test
    void deleteCategory_forbidden_returns403() throws Exception {
        String categoryName = "Salary"; // Attempting to delete a default category by name
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Default categories cannot be deleted."))
            .when(categoryService).deleteCategory(eq(categoryName), eq(userEmail));

        mockMvc.perform(delete("/api/categories/{name}", categoryName).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Default categories cannot be deleted."));
    }

    @Test
    void deleteCategory_conflict_returns409() throws Exception {
        String categoryName = "Food";
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Category is in use and cannot be deleted."))
            .when(categoryService).deleteCategory(eq(categoryName), eq(userEmail));

        mockMvc.perform(delete("/api/categories/{name}", categoryName).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category is in use and cannot be deleted."));
    }
}
