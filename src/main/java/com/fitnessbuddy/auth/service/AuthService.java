package com.fitnessbuddy.auth.service;

import com.fitnessbuddy.auth.dto.LoginRequest;
import com.fitnessbuddy.auth.dto.RegisterRequest;
import com.fitnessbuddy.user.dto.UserResponseDto;

public interface AuthService {

    /** @throws com.fitnessbuddy.common.exception.ConflictException if the email or username is taken */
    UserResponseDto registerUser(RegisterRequest registerRequest);

    /** @throws com.fitnessbuddy.common.exception.UnauthorizedException if the credentials are wrong */
    LoginResult loginUser(LoginRequest loginRequest);
}
