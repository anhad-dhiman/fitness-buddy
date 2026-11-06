package com.fitnessBuddy.fitness_buddy.controllers.auth;

import com.fitnessBuddy.fitness_buddy.domain.dto.UserResponseDto;
import com.fitnessBuddy.fitness_buddy.domain.dto.auth.LoginRequest;
import com.fitnessBuddy.fitness_buddy.domain.dto.auth.RegisterRequest;
import com.fitnessBuddy.fitness_buddy.services.Auth.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {


    private AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public UserResponseDto registerUser(@RequestBody RegisterRequest registerRequest) {
        return authService.registerUser(registerRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(@RequestBody LoginRequest loginRequest,
                                                 HttpServletResponse response) {
        return authService.loginUser(loginRequest, response);
    }
}
