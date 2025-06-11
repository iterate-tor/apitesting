package com.example.financialmanager.controllers;

import com.example.financialmanager.dtos.*;
import com.example.financialmanager.entities.TransactionType;
import com.example.financialmanager.services.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@WithMockUser(username = "user@example.com") // Apply mock user for all tests in this class
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper needs JavaTimeModule for LocalDate
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());


    @MockBean
    private TransactionService transactionService;

    // @MockBean UserDetailsService is not strictly needed here if @WithMockUser is effective for controller tests.
    // It's more for testing security filter chain components with WebMvcTest if needed.

    private TransactionRequestDto validRequestDto;
    private TransactionResponseDto responseDto;
    private UUID transactionId;
    private String userEmail = "user@example.com";

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        validRequestDto = new TransactionRequestDto(BigDecimal.valueOf(100.50), LocalDate.now().minusDays(1), categoryId, "Test Transaction", TransactionType.EXPENSE);
        responseDto = new TransactionResponseDto(transactionId, BigDecimal.valueOf(100.50), LocalDate.now().minusDays(1), "Groceries", "Test Transaction", TransactionType.EXPENSE);
    }

    @Test
    void createTransaction_validRequest_returns201() throws Exception {
        when(transactionService.createTransaction(any(TransactionRequestDto.class), eq(userEmail))).thenReturn(responseDto);

        mockMvc.perform(post("/api/transactions").with(csrf()) // Added csrf for POST
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(responseDto.id().toString()))
                .andExpect(jsonPath("$.categoryName").value(responseDto.categoryName()));
    }

    @Test
    void createTransaction_invalidAmount_returns400() throws Exception {
        TransactionRequestDto invalidDto = new TransactionRequestDto(BigDecimal.valueOf(-50), LocalDate.now(), UUID.randomUUID(), "Invalid", TransactionType.EXPENSE);
        // MockMvc will trigger DTO validation before it reaches the service method in this case
        mockMvc.perform(post("/api/transactions").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").exists()); // Check for amount field error
    }

    @Test
    void createTransaction_serviceThrowsBadRequest_returns400() throws Exception {
        when(transactionService.createTransaction(any(TransactionRequestDto.class), eq(userEmail)))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category ID"));

        mockMvc.perform(post("/api/transactions").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid category ID"));
    }


    @Test
    void getTransactions_returns200AndWrappedResponse() throws Exception {
        List<TransactionResponseDto> dtoList = Collections.singletonList(responseDto);
        when(transactionService.getTransactions(eq(userEmail), any(), any(), any(), any())).thenReturn(dtoList);

        mockMvc.perform(get("/api/transactions")
                .param("categoryId", UUID.randomUUID().toString())) // Example with a param
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions[0].id").value(responseDto.id().toString()));
    }

    @Test
    void getTransactionById_success_returns200() throws Exception {
        when(transactionService.getTransactionById(eq(transactionId), eq(userEmail))).thenReturn(responseDto);

        mockMvc.perform(get("/api/transactions/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId.toString()));
    }

    @Test
    void getTransactionById_notFound_returns404() throws Exception {
        when(transactionService.getTransactionById(eq(transactionId), eq(userEmail)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        mockMvc.perform(get("/api/transactions/{id}", transactionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found"));
    }

    @Test
    void getTransactionById_forbidden_returns403() throws Exception {
        when(transactionService.getTransactionById(eq(transactionId), eq(userEmail)))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(get("/api/transactions/{id}", transactionId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }


    @Test
    void updateTransaction_validRequest_returns200() throws Exception {
        UpdateTransactionRequestDto updateDto = new UpdateTransactionRequestDto(BigDecimal.valueOf(150), null, "Updated", null);
        TransactionResponseDto updatedResponse = new TransactionResponseDto(transactionId, BigDecimal.valueOf(150), responseDto.date(), responseDto.categoryName(), "Updated", responseDto.type());
        when(transactionService.updateTransaction(eq(transactionId), any(UpdateTransactionRequestDto.class), eq(userEmail))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/transactions/{id}", transactionId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(150))
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    void updateTransaction_invalidAmount_returns400() throws Exception {
        UpdateTransactionRequestDto invalidUpdateDto = new UpdateTransactionRequestDto(BigDecimal.valueOf(-20), null, null, null);
        mockMvc.perform(put("/api/transactions/{id}", transactionId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUpdateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").exists());
    }


    @Test
    void deleteTransaction_success_returns200WithMessage() throws Exception {
        doNothing().when(transactionService).deleteTransaction(eq(transactionId), eq(userEmail));

        mockMvc.perform(delete("/api/transactions/{id}", transactionId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted successfully"));
    }

    @Test
    void deleteTransaction_notFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found to delete"))
            .when(transactionService).deleteTransaction(eq(transactionId), eq(userEmail));

        mockMvc.perform(delete("/api/transactions/{id}", transactionId).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found to delete"));
    }
}
