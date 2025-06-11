package com.example.financialmanager.controllers;

import com.example.financialmanager.dtos.RegisterRequest;
import com.example.financialmanager.dtos.UserRegistrationResponseDto;
import com.example.financialmanager.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService; // Required by Spring Security context
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // Spring Security's WebMvcTest auto-configuration often requires a UserDetailsService bean.
    // If AuthService is not directly implementing it or if context needs it explicitly:
    @MockBean
    private UserDetailsService userDetailsService; // This can be the same instance as authService if it implements UserDetailsService

    @Test
    void register_success() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("test@example.com", "password123", "Test User", "1234567890");
        UserRegistrationResponseDto responseDto = new UserRegistrationResponseDto("User registered successfully", UUID.randomUUID().toString());

        when(authService.registerUser(any(RegisterRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(responseDto.message()))
                .andExpect(jsonPath("$.userId").value(responseDto.userId()));
    }

    @Test
    void register_invalidRequest_blankUsername() throws Exception {
        // Username (email) is blank
        RegisterRequest registerRequest = new RegisterRequest("", "password123", "Test User", "");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed")) // Assuming GlobalExceptionHandler is active
                .andExpect(jsonPath("$.details.username").exists()); // Check specific field error
    }

    @Test
    void register_invalidRequest_shortPassword() throws Exception {
        // Password too short
        RegisterRequest registerRequest = new RegisterRequest("test@example.com", "pass", "Test User", "");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.password").value("size must be between 8 and 2147483647"));
    }

    @Test
    void register_conflict() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("conflict@example.com", "password123", "Conflict User", "");

        when(authService.registerUser(any(RegisterRequest.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email (username) already exists"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email (username) already exists"));
    }

    // Tests for /api/auth/login and /api/auth/logout are more complex for @WebMvcTest
    // because Spring Security's filter chain handles them.
    // A simple test might just check if the endpoint is reachable or if it returns 401/200
    // depending on how security context is mocked or handled by default in WebMvcTest.

    @Test
    void login_endpointExists() throws Exception {
        // This test doesn't mock successful login, just that the endpoint can be called.
        // Spring Security will typically handle the request.
        // Depending on test security setup, it might return 401 (unauthorized) if no credentials/mock UserDetailsService is properly configured
        // or if formLogin() is not fully active in the test slice.
        // Here we expect 401 as we are not providing valid credentials through Spring Security's mechanism.
        // The AuthController's own method body for /login returns 401 if reached directly.
        String loginRequestJson = "{\"username\":\"user@example.com\", \"password\":\"password\"}";
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestJson))
                .andExpect(status().isUnauthorized()); // Or another status depending on test security setup
    }

    @Test
    void logout_endpointExists() throws Exception {
        // Similar to login, Spring Security handles logout.
        // The AuthController's own method body for /logout returns 405 if reached directly.
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isMethodNotAllowed()); // Or another status if security processes it
    }
}
