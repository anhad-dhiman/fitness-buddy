package com.fitnessbuddy.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the HttpOnly cookie that carries the JWT. Login and logout both go
 * through here: a browser only deletes a cookie when the "clear" cookie has the
 * same name, path and domain, so building both in one place keeps them in sync.
 */
@Component
public class AccessTokenCookieFactory {

    public static final String COOKIE_NAME = "accessToken";

    private final AppSecurityProperties properties;

    public AccessTokenCookieFactory(AppSecurityProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(String token) {
        return base(token).maxAge(properties.accessTokenTtl()).build();
    }

    /** An empty cookie with Max-Age=0, which tells the browser to delete it. */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)                        // JavaScript can't read it (XSS can't steal it)
                .secure(properties.cookieSecure())     // HTTPS only, except local plain-http dev
                .sameSite("Lax")                       // not sent on cross-site POST/PUT/DELETE
                .path("/");
    }
}
