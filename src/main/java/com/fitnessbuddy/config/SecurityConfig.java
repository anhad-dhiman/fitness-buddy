package com.fitnessbuddy.config;

import com.fitnessbuddy.security.AppSecurityProperties;
import com.fitnessbuddy.security.JwtAuthenticationFilter;
import com.fitnessbuddy.security.JwtService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Stores the CSRF token in the XSRF-TOKEN cookie. JavaScript must be able to
     * read it ("withHttpOnlyFalse"), unlike the accessToken cookie. It is a bean
     * so AuthController can rotate the token at login and logout.
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    /**
     * @param exceptionResolver Spring MVC's exception-handling pipeline. Security
     *        errors happen in filters, before any controller runs, so
     *        {@code @RestControllerAdvice} never sees them on its own. Handing
     *        them to this resolver routes them through GlobalExceptionHandler,
     *        so 401/403 responses use the same Problem Details JSON as every
     *        other error.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            CsrfTokenRepository csrfTokenRepository,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) throws Exception {
        http
                // CSRF protection for a cookie-authenticated single-page app:
                // Spring stores a random token in a JavaScript-readable XSRF-TOKEN
                // cookie, and every POST/PUT/PATCH/DELETE must echo it back in the
                // X-XSRF-TOKEN header. Another website can make the browser *send*
                // our cookies but cannot *read* them, so it can't produce the header.
                .csrf(csrf -> csrf
                        .spa()
                        .csrfTokenRepository(csrfTokenRepository)
                        // By default Spring replaces the CSRF token whenever a user
                        // "logs in". Without an HTTP session it can't tell that a
                        // JWT-authenticated request belongs to an already logged-in
                        // user, so it would replace the token on EVERY request.
                        // AuthController rotates it itself at login and logout instead.
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))

                // Uses the CorsConfigurationSource bean from CorsConfig.
                .cors(Customizer.withDefaults())

                // Identity comes from the JWT on every request; never create
                // an HTTP session (JSESSIONID).
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/csrf").permitAll()
                        // Spring Boot forwards unhandled errors to /error. If it
                        // required authentication, every error would be masked as 403.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )

                .exceptionHandling(errors -> errors
                        // Not authenticated (no/invalid/expired token) -> 401
                        .authenticationEntryPoint((request, response, ex) ->
                                exceptionResolver.resolveException(request, response, null, ex))
                        // Authenticated but not allowed, or bad CSRF token -> 403
                        .accessDeniedHandler((request, response, ex) ->
                                exceptionResolver.resolveException(request, response, null, ex))
                )

                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
