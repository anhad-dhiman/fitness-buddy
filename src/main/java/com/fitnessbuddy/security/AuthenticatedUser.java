package com.fitnessbuddy.security;

import java.security.Principal;

/**
 * The identity of the caller, as proven by a valid JWT. JwtAuthenticationFilter
 * stores it as the principal in the SecurityContext; controllers receive it
 * with {@code @AuthenticationPrincipal AuthenticatedUser user}.
 *
 * Always take "who is making this request" from here, never from an id in
 * the request body or URL that the client could change.
 */
public record AuthenticatedUser(Integer id, String role) implements Principal {

    /** Used by Spring Security's {@code Authentication.getName()}, e.g. in logs. */
    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
