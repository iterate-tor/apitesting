package com.example.financialmanager.controllers;

import com.example.financialmanager.dtos.LoginRequest;
import com.example.financialmanager.dtos.RegisterRequest;
// import com.example.financialmanager.dtos.UserDto; // No longer returning UserDto
import com.example.financialmanager.dtos.UserRegistrationResponseDto; // New return type
import com.example.financialmanager.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponseDto> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserRegistrationResponseDto responseDto = authService.registerUser(registerRequest);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest loginRequest) {
        // This method is effectively a placeholder.
        // Spring Security handles the actual login process via formLogin().loginProcessingUrl("/api/auth/login").
        // If this endpoint is reached, it means Spring Security did not intercept the request,
        // which could indicate a configuration issue or that the request was not a form submission
        // matching the loginProcessingUrl.
        // Returning 401 Unauthorized is a sensible default if direct access occurs.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"message\": \"Direct access to login endpoint not allowed. Use form submission to /api/auth/login.\"}");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // This method is effectively a placeholder.
        // Spring Security handles the actual logout process via logout().logoutUrl("/api/auth/logout").
        // Similar to login, direct access should ideally not happen if Spring Security is configured correctly.
        // Returning an informative message or an error status can be appropriate.
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("{\"message\": \"Direct access to logout endpoint not allowed. Spring Security handles logout at /api/auth/logout.\"}");
    }
}
