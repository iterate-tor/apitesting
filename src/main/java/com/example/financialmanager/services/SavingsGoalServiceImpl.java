package com.example.financialmanager.services;

import com.example.financialmanager.dtos.SavingsGoalRequestDto;
import com.example.financialmanager.dtos.SavingsGoalResponseDto;
import com.example.financialmanager.entities.SavingsGoal;
import com.example.financialmanager.entities.User;
import com.example.financialmanager.repositories.SavingsGoalRepository;
import com.example.financialmanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SavingsGoalServiceImpl implements SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;

    @Autowired
    public SavingsGoalServiceImpl(SavingsGoalRepository savingsGoalRepository, UserRepository userRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userRepository = userRepository;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    private SavingsGoalResponseDto convertToDto(SavingsGoal goal) {
        return new SavingsGoalResponseDto(
            goal.getId(),
            goal.getName(),
            goal.getTargetAmount(),
            goal.getCurrentAmount(),
            goal.getTargetDate(),
            goal.getUser().getId()
        );
    }

    @Override
    @Transactional
    public SavingsGoalResponseDto createGoal(SavingsGoalRequestDto requestDto, String userEmail) {
        User user = getUserByEmail(userEmail);

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName(requestDto.name());
        goal.setTargetAmount(requestDto.targetAmount());
        goal.setTargetDate(requestDto.targetDate());
        goal.setCurrentAmount(requestDto.currentAmount() == null ? BigDecimal.ZERO : requestDto.currentAmount());

        SavingsGoal savedGoal = savingsGoalRepository.save(goal);
        return convertToDto(savedGoal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsGoalResponseDto> getAllGoals(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<SavingsGoal> goals = savingsGoalRepository.findByUser(user);
        return goals.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SavingsGoalResponseDto getGoalById(UUID goalId, String userEmail) {
        User user = getUserByEmail(userEmail);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUser(goalId, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found or access denied"));
        return convertToDto(goal);
    }

    @Override
    @Transactional
    public SavingsGoalResponseDto updateGoal(UUID goalId, SavingsGoalRequestDto requestDto, String userEmail) {
        User user = getUserByEmail(userEmail);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUser(goalId, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found or access denied"));

        goal.setName(requestDto.name());
        goal.setTargetAmount(requestDto.targetAmount());
        goal.setTargetDate(requestDto.targetDate());
        if (requestDto.currentAmount() != null) {
            goal.setCurrentAmount(requestDto.currentAmount());
        }
        // If currentAmount is null in DTO, existing currentAmount is preserved.

        SavingsGoal updatedGoal = savingsGoalRepository.save(goal);
        return convertToDto(updatedGoal);
    }

    @Override
    @Transactional
    public void deleteGoal(UUID goalId, String userEmail) {
        User user = getUserByEmail(userEmail);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUser(goalId, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found or access denied"));
        savingsGoalRepository.delete(goal);
    }
}
