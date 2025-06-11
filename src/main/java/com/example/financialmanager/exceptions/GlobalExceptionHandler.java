package com.example.financialmanager.exceptions;

import com.example.financialmanager.dtos.ErrorResponseDto;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error -> // For object-level errors
            errors.put(error.getObjectName(), error.getDefaultMessage()));

        return new ErrorResponseDto(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "Request body validation failed. Check 'details' for specific errors.",
            request.getDescription(false).replace("uri=", ""),
            errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class) // For @Validated @PathVariable, @RequestParam
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        List<String> errors = ex.getConstraintViolations().stream()
            .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
            .collect(Collectors.toList());

        // Using a Map for details to be consistent with MethodArgumentNotValidException handling,
        // though a List could also work if ErrorResponseDto.details is flexible.
        Map<String, List<String>> details = new HashMap<>();
        details.put("parameterViolations", errors);

        return new ErrorResponseDto(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "Request parameter/path variable validation failed. Check 'details' for specific errors.",
            request.getDescription(false).replace("uri=", ""),
            details // Passing the map here
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();
        ErrorResponseDto errorResponse = new ErrorResponseDto(
            LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(), // Standard reason for the status
            ex.getReason(), // Custom message from the exception constructor
            request.getDescription(false).replace("uri=", ""),
            null
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(AccessDeniedException.class) // org.springframework.security.access.AccessDeniedException
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponseDto handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        return new ErrorResponseDto(
            LocalDateTime.now(),
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            ex.getMessage() != null ? ex.getMessage() : "You do not have permission to access this resource.",
            request.getDescription(false).replace("uri=", ""),
            null
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class) // org.springframework.dao.DataIntegrityViolationException
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDto handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        String message = "Data integrity violation. This could be due to a duplicate entry or other constraint violation.";
        // Attempt to get a more specific message from the cause
        Throwable cause = ex.getCause();
        if (cause instanceof org.hibernate.exception.ConstraintViolationException) {
            // The message from Hibernate's ConstraintViolationException can be quite detailed
            // and might expose too much (e.g. constraint names). Consider logging it and providing a more generic user message.
            // For now, using a slightly more detailed message if available.
            message = "Database constraint violation: " + cause.getMessage();
        } else if (ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage() != null && !ex.getMostSpecificCause().getMessage().isEmpty()) {
            message = "Data integrity issue: " + ex.getMostSpecificCause().getMessage();
        }


        return new ErrorResponseDto(
            LocalDateTime.now(),
            HttpStatus.CONFLICT.value(),
            "Conflict",
            message,
            request.getDescription(false).replace("uri=", ""),
            null
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDto handleAllUncaughtException(Exception ex, WebRequest request) {
        // It's good practice to log the exception here
        // ex.printStackTrace(); // Or use a proper logger: log.error("Unhandled exception:", ex);
        System.err.println("Unhandled exception: " + ex.getMessage()); // Basic logging for now
        ex.printStackTrace();


        return new ErrorResponseDto(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            request.getDescription(false).replace("uri=", ""),
            null
        );
    }
}
