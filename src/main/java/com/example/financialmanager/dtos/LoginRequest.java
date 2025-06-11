package com.example.financialmanager.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank @Email String username, // Changed from email to username to match spec
    @NotBlank String password
) {}
