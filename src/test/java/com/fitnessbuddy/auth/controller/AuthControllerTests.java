package com.fitnessbuddy.auth.controller;

import com.fitnessbuddy.auth.service.AuthService;
import com.fitnessbuddy.auth.service.LoginResult;
import com.fitnessbuddy.common.exception.ConflictException;
import com.fitnessbuddy.common.exception.UnauthorizedException;
import com.fitnessbuddy.config.SecurityConfig;
import com.fitnessbuddy.security.AccessTokenCookieFactory;
import com.fitnessbuddy.security.JwtService;
import com.fitnessbuddy.user.dto.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer slice test: starts only MVC infrastructure (controllers, JSON,
 * validation, the @RestControllerAdvice, and the security filter chain we
 * import) with the service mocked. It checks the HTTP contract: status codes,
 * response bodies, validation messages, and cookies.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AccessTokenCookieFactory.class})
class AuthControllerTests {

    @Autowired MockMvc mockMvc;

    @MockitoBean AuthService authService;
    @MockitoBean JwtService jwtService; // needed by JwtAuthenticationFilter

    @Test
    void registerReturns201WithTheUser() throws Exception {
        when(authService.registerUser(any())).thenReturn(new UserResponseDto("anhad", "USER"));

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"anhad","email":"anhad@example.com","password":"secret123"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value("anhad"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerWithInvalidFieldsReturns400ProblemWithEveryFieldError() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"a!","email":"not-an-email"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.password[0]").value("Password is required"))
                .andExpect(jsonPath("$.errors.email[0]").value("Email must be a valid email address"))
                .andExpect(jsonPath("$.errors.userName.length()").value(2));

        verifyNoInteractions(authService); // validation stops the request before the service
    }

    @Test
    void registerRejectsPasswordLongerThan72Bytes() throws Exception {
        String emojiPassword = "💪".repeat(20); // 40 Java chars, but 80 UTF-8 bytes

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"anhad","email":"anhad@example.com","password":"%s"}""".formatted(emojiPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password[0]").value("Password is too long"));
    }

    @Test
    void registerWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"userName\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void registerConflictReturns409Problem() throws Exception {
        when(authService.registerUser(any())).thenThrow(new ConflictException("An account with this email already exists"));

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"anhad","email":"anhad@example.com","password":"secret123"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("An account with this email already exists"))
                .andExpect(jsonPath("$.instance").value("/auth/register"));
    }

    @Test
    void loginSetsHttpOnlyAccessTokenCookie() throws Exception {
        when(authService.loginUser(any())).thenReturn(new LoginResult("jwt-token", new UserResponseDto("anhad", "USER")));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"anhad@example.com","password":"secret123"}"""))
                .andExpect(status().isOk())
                .andExpect(cookie().value("accessToken", "jwt-token"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(jsonPath("$.userName").value("anhad"));
    }

    @Test
    void loginWithBadCredentialsReturns401Problem() throws Exception {
        when(authService.loginUser(any())).thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"anhad@example.com","password":"wrong-password"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"))
                .andExpect(cookie().doesNotExist("accessToken"));
    }

    @Test
    void loginWithMissingFieldsReturns400() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email[0]").value("Email is required"))
                .andExpect(jsonPath("$.errors.password[0]").value("Password is required"));
    }
}
