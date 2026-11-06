package com.fitnessbuddy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates a request from the accessToken cookie, if it holds a valid JWT.
 * With no or an invalid token it does nothing, and the request continues as
 * anonymous: the authorization rules in SecurityConfig then decide (401 for
 * protected endpoints).
 *
 * Deliberately NOT a {@code @Component}: Spring Boot automatically registers
 * every Filter bean in the servlet container, so a {@code @Component} filter
 * would run both there and inside the security chain. SecurityConfig creates it
 * with {@code new} and adds it to the security chain only.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            jwtService.parse(token).ifPresent(user -> {
                var authentication = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));

                // A fresh context instead of mutating the current one, as the
                // Spring Security docs recommend, to avoid races between threads.
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            });
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(AccessTokenCookieFactory.COOKIE_NAME)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
