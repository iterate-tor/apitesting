package com.example.financialmanager.dtos;

import java.util.UUID;

public record UserDto(
    UUID id,
    String email,
    String fullName,
    String phone
) {}
