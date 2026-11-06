package com.fitnessbuddy.auth.service;

import com.fitnessbuddy.auth.dto.LoginRequest;
import com.fitnessbuddy.auth.dto.RegisterRequest;
import com.fitnessbuddy.common.exception.ConflictException;
import com.fitnessbuddy.common.exception.UnauthorizedException;
import com.fitnessbuddy.security.JwtService;
import com.fitnessbuddy.user.entity.UserEntity;
import com.fitnessbuddy.user.mapper.UserMapperImpl;
import com.fitnessbuddy.user.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests: no Spring context, no database. Collaborators are Mockito
 * mocks, so each test controls exactly what the repository "returns" and
 * runs in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTests {

    @Mock UserRepo userRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepo, passwordEncoder,
                new UserMapperImpl(new ModelMapper()), jwtService);
    }

    // ------------------------------------------------------------ register --

    @Test
    void registerNormalizesEmailHashesPasswordAndSaves() {
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");

        var response = authService.registerUser(new RegisterRequest("anhad", "secret123", "  Anhad@Example.COM "));

        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepo).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("anhad@example.com");
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed");
        assertThat(saved.getValue().getRole()).isEqualTo("USER");
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
        assertThat(response.getUserName()).isEqualTo("anhad");
    }

    @Test
    void registerRejectsExistingEmail() {
        when(userRepo.existsByEmailIgnoreCase("anhad@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(new RegisterRequest("anhad", "secret123", "anhad@example.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("email");
        verify(userRepo, never()).saveAndFlush(any());
    }

    @Test
    void registerRejectsTakenUserName() {
        when(userRepo.existsByUserNameIgnoreCase("anhad")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(new RegisterRequest("anhad", "secret123", "anhad@example.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("username");
        verify(userRepo, never()).saveAndFlush(any());
    }

    @Test
    void registerTurnsUniqueConstraintRaceIntoConflict() {
        // Both exists-checks passed, but another request inserted the same
        // email in between, so the database unique index rejects our insert.
        when(userRepo.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("ux_users_email"));

        assertThatThrownBy(() -> authService.registerUser(new RegisterRequest("anhad", "secret123", "anhad@example.com")))
                .isInstanceOf(ConflictException.class);
    }

    // --------------------------------------------------------------- login --

    @Test
    void loginWithUnknownEmailIsUnauthorized() {
        when(userRepo.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginUser(new LoginRequest("secret123", "nobody@example.com")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginWithUnknownEmailStillRunsThePasswordCheck() {
        // Timing equalisation: BCrypt must run even when the email doesn't
        // exist, so response time can't reveal which emails are registered.
        when(userRepo.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginUser(new LoginRequest("secret123", "nobody@example.com")))
                .isInstanceOf(UnauthorizedException.class);
        verify(passwordEncoder).matches(eq("secret123"), any());
    }

    @Test
    void loginWithWrongPasswordIsUnauthorizedWithTheSameMessage() {
        when(userRepo.findByEmailIgnoreCase("anhad@example.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.loginUser(new LoginRequest("wrong-password", "anhad@example.com")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
        verify(jwtService, never()).generateJwtToken(any());
    }

    @Test
    void loginSuccessReturnsTokenAndUpdatesAccessedAt() {
        UserEntity user = user();
        Instant before = user.getAccessedAt();
        when(userRepo.findByEmailIgnoreCase("anhad@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtService.generateJwtToken(user)).thenReturn("jwt-token");

        LoginResult result = authService.loginUser(new LoginRequest("secret123", " ANHAD@example.com"));

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.user().getUserName()).isEqualTo("anhad");
        assertThat(user.getAccessedAt()).isAfter(before);
    }

    private static UserEntity user() {
        return UserEntity.builder()
                .id(1).userName("anhad").email("anhad@example.com").password("hashed").role("USER")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .accessedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
