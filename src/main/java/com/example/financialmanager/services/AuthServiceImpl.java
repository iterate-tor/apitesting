package com.example.financialmanager.services;

import com.example.financialmanager.dtos.RegisterRequest;
// import com.example.financialmanager.dtos.UserDto; // No longer returning UserDto
import com.example.financialmanager.dtos.UserRegistrationResponseDto; // New return type
import com.example.financialmanager.entities.User;
import com.example.financialmanager.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList; // For UserDetails authorities

@Service
public class AuthServiceImpl implements AuthService, UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // UserRepository is already autowired via constructor. No need for a separate field injection.

    @Override
    public UserRegistrationResponseDto registerUser(RegisterRequest registerRequest) {
        if (userRepository.findByEmail(registerRequest.username()).isPresent()) { // Use username() here
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email (username) already exists");
        }

        User user = new User();
        user.setEmail(registerRequest.username()); // Use username() for entity's email field
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setFullName(registerRequest.fullName());
        user.setPhone(registerRequest.phone());

        User savedUser = userRepository.save(user);

        return new UserRegistrationResponseDto("User registered successfully", savedUser.getId());
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username)); // parameter renamed

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), // This is correct, User entity stores it as email
            user.getPassword(),
            new ArrayList<>() // No authorities/roles defined yet
        );
    }
}
