package com.example.financialmanager.services;

import com.example.financialmanager.dtos.RegisterRequest;
import com.example.financialmanager.dtos.UserDto;

public interface AuthService {
    UserDto registerUser(RegisterRequest registerRequest);
}
