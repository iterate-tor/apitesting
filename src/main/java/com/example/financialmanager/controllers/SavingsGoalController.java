package com.example.financialmanager.controllers;

import com.example.financialmanager.dtos.SavingsGoalRequestDto;
import com.example.financialmanager.dtos.SavingsGoalResponseDto;
import com.example.financialmanager.dtos.UpdateSavingsGoalRequestDto; // Added
import com.example.financialmanager.dtos.MessageResponseDto; // Added
import com.example.financialmanager.services.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map; // Added
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    @Autowired
    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping
    public ResponseEntity<SavingsGoalResponseDto> createGoal(@Valid @RequestBody SavingsGoalRequestDto goalDto) {
        String userEmail = getCurrentUserEmail();
        SavingsGoalResponseDto createdGoal = savingsGoalService.createGoal(goalDto, userEmail);
        return new ResponseEntity<>(createdGoal, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Map<String, List<SavingsGoalResponseDto>>> getAllGoals() { // Response wrapped in Map
        String userEmail = getCurrentUserEmail();
        List<SavingsGoalResponseDto> goals = savingsGoalService.getAllGoals(userEmail);
        Map<String, List<SavingsGoalResponseDto>> response = Map.of("goals", goals); // Wrapped response
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsGoalResponseDto> getGoalById(@PathVariable UUID id) {
        String userEmail = getCurrentUserEmail();
        SavingsGoalResponseDto goal = savingsGoalService.getGoalById(id, userEmail);
        return ResponseEntity.ok(goal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalResponseDto> updateGoal(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSavingsGoalRequestDto goalDto) { // Changed DTO type
        String userEmail = getCurrentUserEmail();
        SavingsGoalResponseDto updatedGoal = savingsGoalService.updateGoal(id, goalDto, userEmail);
        return ResponseEntity.ok(updatedGoal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponseDto> deleteGoal(@PathVariable UUID id) { // Return type changed
        String userEmail = getCurrentUserEmail();
        savingsGoalService.deleteGoal(id, userEmail);
        return ResponseEntity.ok(new MessageResponseDto("Goal deleted successfully")); // New response body
    }
}
