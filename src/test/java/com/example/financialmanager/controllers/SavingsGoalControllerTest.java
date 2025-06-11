package com.example.financialmanager.controllers;

import com.example.financialmanager.dtos.*;
import com.example.financialmanager.services.SavingsGoalService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SavingsGoalController.class)
@WithMockUser(username = "user@example.com")
class SavingsGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockBean
    private SavingsGoalService savingsGoalService;

    private String userEmail = "user@example.com";
    private SavingsGoalRequestDto validCreateRequestDto;
    private UpdateSavingsGoalRequestDto validUpdateRequestDto;
    private SavingsGoalResponseDto responseDto;
    private UUID goalId;

    @BeforeEach
    void setUp() {
        goalId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusMonths(3);

        validCreateRequestDto = new SavingsGoalRequestDto("Beach Vacation", BigDecimal.valueOf(2000), futureDate, today);
        validUpdateRequestDto = new UpdateSavingsGoalRequestDto(BigDecimal.valueOf(2500), futureDate.plusMonths(1));

        responseDto = new SavingsGoalResponseDto(
                goalId,
                "Beach Vacation",
                BigDecimal.valueOf(2000.00).setScale(2),
                futureDate,
                today,
                BigDecimal.ZERO.setScale(2),
                0.0,
                BigDecimal.valueOf(2000.00).setScale(2)
        );
    }

    @Test
    void createGoal_validRequest_returns201() throws Exception {
        when(savingsGoalService.createGoal(any(SavingsGoalRequestDto.class), eq(userEmail))).thenReturn(responseDto);

        mockMvc.perform(post("/api/goals").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCreateRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(responseDto.id().toString()))
                .andExpect(jsonPath("$.goalName").value(responseDto.goalName()));
    }

    @Test
    void createGoal_invalidRequest_blankName_returns400() throws Exception {
        SavingsGoalRequestDto invalidDto = new SavingsGoalRequestDto("", BigDecimal.valueOf(100), LocalDate.now().plusDays(10), null);
        mockMvc.perform(post("/api/goals").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.goalName").value("must not be blank"));
    }

    @Test
    void createGoal_serviceThrowsBadRequest_returns400() throws Exception {
        when(savingsGoalService.createGoal(any(SavingsGoalRequestDto.class), eq(userEmail)))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target date must be in the future."));

        mockMvc.perform(post("/api/goals").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCreateRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Target date must be in the future."));
    }

    @Test
    void getAllGoals_returns200AndWrappedResponse() throws Exception {
        List<SavingsGoalResponseDto> dtoList = Collections.singletonList(responseDto);
        when(savingsGoalService.getAllGoals(eq(userEmail))).thenReturn(dtoList);

        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.goals").isArray())
                .andExpect(jsonPath("$.goals[0].id").value(responseDto.id().toString()));
    }

    @Test
    void getGoalById_success_returns200() throws Exception {
        when(savingsGoalService.getGoalById(eq(goalId), eq(userEmail))).thenReturn(responseDto);

        mockMvc.perform(get("/api/goals/{id}", goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(goalId.toString()));
    }

    @Test
    void getGoalById_notFound_returns404() throws Exception {
        when(savingsGoalService.getGoalById(eq(goalId), eq(userEmail)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found"));

        mockMvc.perform(get("/api/goals/{id}", goalId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Savings goal not found"));
    }

    @Test
    void updateGoal_validRequest_returns200() throws Exception {
        SavingsGoalResponseDto updatedResponse = new SavingsGoalResponseDto(
            goalId, "Updated Goal", BigDecimal.valueOf(2500.00).setScale(2), responseDto.targetDate().plusMonths(1),
            responseDto.startDate(), BigDecimal.ZERO.setScale(2), 0.0, BigDecimal.valueOf(2500.00).setScale(2)
        );
        when(savingsGoalService.updateGoal(eq(goalId), any(UpdateSavingsGoalRequestDto.class), eq(userEmail))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/goals/{id}", goalId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUpdateRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount").value(2500.00))
                .andExpect(jsonPath("$.targetDate").value(responseDto.targetDate().plusMonths(1).toString()));
    }

    @Test
    void updateGoal_invalidRequest_negativeAmount_returns400() throws Exception {
        UpdateSavingsGoalRequestDto invalidUpdate = new UpdateSavingsGoalRequestDto(BigDecimal.valueOf(-100), null);
         mockMvc.perform(put("/api/goals/{id}", goalId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUpdate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.targetAmount").exists());
    }


    @Test
    void deleteGoal_success_returns200WithMessage() throws Exception {
        doNothing().when(savingsGoalService).deleteGoal(eq(goalId), eq(userEmail));

        mockMvc.perform(delete("/api/goals/{id}", goalId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Goal deleted successfully"));
    }

    @Test
    void deleteGoal_notFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found to delete"))
            .when(savingsGoalService).deleteGoal(eq(goalId), eq(userEmail));

        mockMvc.perform(delete("/api/goals/{id}", goalId).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Goal not found to delete"));
    }
}
