package com.fitnessbuddy.auth.service;

import com.fitnessbuddy.user.dto.UserResponseDto;

/**
 * What a successful login produces. The service returns this plain value; the
 * controller decides how it travels over HTTP (the token as an HttpOnly
 * cookie, the user as the JSON body). This is internal and never serialised.
 */
public record LoginResult(String accessToken, UserResponseDto user) {
}
