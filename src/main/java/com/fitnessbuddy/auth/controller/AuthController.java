package com.fitnessbuddy.auth.controller;

import com.fitnessbuddy.auth.dto.LoginRequest;
import com.fitnessbuddy.auth.dto.RegisterRequest;
import com.fitnessbuddy.auth.service.AuthService;
import com.fitnessbuddy.auth.service.LoginResult;
import com.fitnessbuddy.security.AccessTokenCookieFactory;
import com.fitnessbuddy.user.dto.UserResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AccessTokenCookieFactory accessTokenCookieFactory;

    public AuthController(AuthService authService, AccessTokenCookieFactory accessTokenCookieFactory) {
        this.authService = authService;
        this.accessTokenCookieFactory = accessTokenCookieFactory;
    }

    // @Valid runs the Bean Validation annotations on the DTO before this method
    // is entered; failures become a 400 in GlobalExceptionHandler.
    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResult result = authService.loginUser(loginRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookieFactory.create(result.accessToken()).toString())
                .body(result.user());
    }
}
