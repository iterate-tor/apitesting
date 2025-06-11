package com.example.financialmanager.services;

import com.example.financialmanager.dtos.RegisterRequest;
// import com.example.financialmanager.dtos.UserDto; // No longer returning UserDto
import com.example.financialmanager.dtos.UserRegistrationResponseDto; // New return type

public interface AuthService {
    UserRegistrationResponseDto registerUser(RegisterRequest registerRequest);
}
