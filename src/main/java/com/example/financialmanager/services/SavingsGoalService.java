package com.example.financialmanager.services;

import com.example.financialmanager.dtos.SavingsGoalRequestDto;
import com.example.financialmanager.dtos.SavingsGoalResponseDto;

import java.util.List;
import java.util.UUID;

public interface SavingsGoalService {

    SavingsGoalResponseDto createGoal(SavingsGoalRequestDto requestDto, String userEmail);

    List<SavingsGoalResponseDto> getAllGoals(String userEmail);

    SavingsGoalResponseDto getGoalById(UUID goalId, String userEmail);

    SavingsGoalResponseDto updateGoal(UUID goalId, com.example.financialmanager.dtos.UpdateSavingsGoalRequestDto requestDto, String userEmail); // Changed DTO type

    void deleteGoal(UUID goalId, String userEmail);
}
