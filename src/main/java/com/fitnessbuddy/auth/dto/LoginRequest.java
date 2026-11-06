package com.fitnessbuddy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deliberately only checks presence. Login must not reveal the registration
 * rules (length, format): a wrong-but-well-formed and a malformed password
 * should look the same to an attacker, which is 401 "Invalid email or password".
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Email is required")
    private String email;
}
