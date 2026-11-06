package com.fitnessbuddy.auth.service;

import com.fitnessbuddy.user.dto.UserResponseDto;
import com.fitnessbuddy.auth.dto.LoginRequest;
import com.fitnessbuddy.auth.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    UserResponseDto registerUser(RegisterRequest registerRequest);
    ResponseEntity<UserResponseDto> loginUser(LoginRequest loginRequest,
                                              HttpServletResponse response);
}
