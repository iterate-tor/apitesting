package com.example.financialmanager.controllers;

import com.example.financialmanager.dtos.SavingsGoalRequestDto;
import com.example.financialmanager.dtos.SavingsGoalResponseDto;
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
        // Ensure authentication is present, though Spring Security should handle unauthorized access.
        // Consider adding a check if SecurityContextHolder.getContext().getAuthentication() is null or not authenticated.
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping
    public ResponseEntity<SavingsGoalResponseDto> createGoal(@Valid @RequestBody SavingsGoalRequestDto goalDto) {
        String userEmail = getCurrentUserEmail();
        SavingsGoalResponseDto createdGoal = savingsGoalService.createGoal(goalDto, userEmail);
        return new ResponseEntity<>(createdGoal, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SavingsGoalResponseDto>> getAllGoals() {
        String userEmail = getCurrentUserEmail();
        List<SavingsGoalResponseDto> goals = savingsGoalService.getAllGoals(userEmail);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsGoalResponseDto> getGoalById(@PathVariable UUID id) {
        String userEmail = getCurrentUserEmail();
        SavingsGoalResponseDto goal = savingsGoalService.getGoalById(id, userEmail);
        return ResponseEntity.ok(goal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalResponseDto> updateGoal(@PathVariable UUID id, @Valid @RequestBody SavingsGoalRequestDto goalDto) {
        String userEmail = getCurrentUserEmail();
        SavingsGoalResponseDto updatedGoal = savingsGoalService.updateGoal(id, goalDto, userEmail);
        return ResponseEntity.ok(updatedGoal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable UUID id) {
        String userEmail = getCurrentUserEmail();
        savingsGoalService.deleteGoal(id, userEmail);
        return ResponseEntity.noContent().build();
    }
}
