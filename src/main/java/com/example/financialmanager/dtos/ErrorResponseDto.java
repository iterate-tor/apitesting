package com.example.financialmanager.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
// Details can be Map<String, String> for field errors, or List<String> for other violations,
// or even a simple String. Using Object type for flexibility.
// import java.util.List;
// import java.util.Map;

public record ErrorResponseDto(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    Object details // Can be Map<String, String>, List<String>, or other structures
) {}
