package com.example.financialmanager.services;

import com.example.financialmanager.dtos.RegisterRequest;
import com.example.financialmanager.dtos.UserRegistrationResponseDto;
import com.example.financialmanager.entities.User;
import com.example.financialmanager.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        registerRequest = new RegisterRequest(
                "testuser@example.com",
                "password123",
                "Test User",
                "1234567890"
        );
        user = new User(
                "testuser@example.com",
                "encodedPassword",
                "Test User",
                "1234567890"
        );
        user.setId(userId);
    }

    @Test
    void registerUser_success() {
        when(userRepository.findByEmail(registerRequest.username())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserRegistrationResponseDto response = authService.registerUser(registerRequest);

        assertNotNull(response);
        assertEquals("User registered successfully", response.message());
        assertEquals(userId.toString(), response.userId());

        verify(userRepository).findByEmail(registerRequest.username());
        verify(passwordEncoder).encode(registerRequest.password());
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getEmail().equals(registerRequest.username()) &&
                savedUser.getPassword().equals("encodedPassword") &&
                savedUser.getFullName().equals(registerRequest.fullName())
        ));
    }

    @Test
    void registerUser_emailAlreadyExists() {
        when(userRepository.findByEmail(registerRequest.username())).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.registerUser(registerRequest);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Email (username) already exists"));

        verify(userRepository).findByEmail(registerRequest.username());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loadUserByUsername_userFound() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = authService.loadUserByUsername("testuser@example.com");

        assertNotNull(userDetails);
        assertEquals(user.getEmail(), userDetails.getUsername());
        assertEquals(user.getPassword(), userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().isEmpty());

        verify(userRepository).findByEmail("testuser@example.com");
    }

    @Test
    void loadUserByUsername_userNotFound() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            authService.loadUserByUsername("nonexistent@example.com");
        });

        assertTrue(exception.getMessage().contains("User not found with username: nonexistent@example.com"));
        verify(userRepository).findByEmail("nonexistent@example.com");
    }
}
