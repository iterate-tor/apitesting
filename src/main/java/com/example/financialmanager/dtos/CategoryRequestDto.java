package com.example.financialmanager.dtos;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequestDto(
    @NotBlank String name
) {}
