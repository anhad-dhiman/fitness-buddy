package com.fitnessbuddy.config;

import com.fitnessbuddy.security.AppSecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS: which *other origins* a browser may let call this API.
 *
 * During development React runs on http://localhost:5173 and the API on
 * http://localhost:8080. Different ports are different origins, so the browser
 * blocks the React app from reading API responses unless the API explicitly
 * allows that origin. CORS is enforced by browsers only; it is not an
 * authentication mechanism and does not stop curl or Postman.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppSecurityProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        // Exact origins only. With credentials (cookies) allowed, a wildcard "*"
        // origin is forbidden by the CORS spec, and it would be unsafe anyway.
        config.setAllowedOrigins(properties.corsAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        // Let the browser send and accept cookies (accessToken, XSRF-TOKEN) cross-origin.
        config.setAllowCredentials(true);
        // Browsers may cache the preflight (OPTIONS) answer for an hour.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
