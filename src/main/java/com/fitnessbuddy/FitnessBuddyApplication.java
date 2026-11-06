package com.fitnessbuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Authentication is JWT-based (JwtAuthenticationFilter), so Spring Boot's default
// in-memory user ("Using generated security password: ...") is not wanted.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class FitnessBuddyApplication {

	public static void main(String[] args) {
		SpringApplication.run(FitnessBuddyApplication.class, args);
	}

}
