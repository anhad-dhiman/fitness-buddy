package com.fitnessbuddy.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Typed, validated view of the {@code app.security.*} properties in
 * application.yml. Binding them to a record (instead of scattering
 * {@code @Value("${...}")} around) gives one place to see every security
 * setting, and {@code @Validated} makes the application refuse to start
 * when a required one is missing.
 *
 * @param jwtSecret          Base64-encoded HMAC key, at least 32 bytes. Comes from the
 *                           JWT_SECRET environment variable; never commit a real one.
 * @param accessTokenTtl     lifetime of the JWT and of the cookie that carries it
 * @param cookieSecure       send the auth cookie over HTTPS only. Must be true in any
 *                           deployed environment; false only for plain-http local dev.
 * @param corsAllowedOrigins browser origins (scheme://host:port) of the frontend that may
 *                           call this API with cookies
 */
@Validated
@ConfigurationProperties("app.security")
public record AppSecurityProperties(
        @NotBlank String jwtSecret,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("true") boolean cookieSecure,
        @NotEmpty List<String> corsAllowedOrigins
) {
}
