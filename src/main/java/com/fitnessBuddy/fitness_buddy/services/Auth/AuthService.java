package com.fitnessBuddy.fitness_buddy.services.Auth;

import com.fitnessBuddy.fitness_buddy.domain.dto.UserResponseDto;
import com.fitnessBuddy.fitness_buddy.domain.dto.auth.LoginRequest;
import com.fitnessBuddy.fitness_buddy.domain.dto.auth.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    UserResponseDto registerUser(RegisterRequest registerRequest);
    ResponseEntity<UserResponseDto> loginUser(LoginRequest loginRequest,
                                              HttpServletResponse response);
}
