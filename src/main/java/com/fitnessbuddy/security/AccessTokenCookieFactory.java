package com.fitnessbuddy.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the HttpOnly cookie that carries the JWT. It lives in one place so
 * that login (and later logout, which must clear a cookie with the exact same
 * name/path) can't drift apart.
 *
 * Values are unchanged from the original login code. Making Secure depend
 * on the environment is part of Milestone 1.4.
 */
@Component
public class AccessTokenCookieFactory {

    public static final String COOKIE_NAME = "accessToken";

    public ResponseCookie create(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .build();
    }
}
