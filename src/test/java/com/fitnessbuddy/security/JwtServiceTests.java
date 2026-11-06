package com.fitnessbuddy.security;

import com.fitnessbuddy.user.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTests {

    private static final String KEY_A = Base64.getEncoder().encodeToString("a".repeat(32).getBytes());
    private static final String KEY_B = Base64.getEncoder().encodeToString("b".repeat(32).getBytes());

    private static JwtService jwtService(String key, Duration ttl) {
        return new JwtService(new AppSecurityProperties(key, ttl, true, List.of("http://localhost:5173")));
    }

    private static UserEntity user() {
        return UserEntity.builder().id(42).role("USER").email("anhad@example.com").build();
    }

    @Test
    void generatedTokenParsesBackToTheSameIdentity() {
        JwtService jwt = jwtService(KEY_A, Duration.ofMinutes(15));

        assertThat(jwt.parse(jwt.generateJwtToken(user())))
                .contains(new AuthenticatedUser(42, "USER"));
    }

    @Test
    void tokenDoesNotContainTheEmail() {
        String token = jwtService(KEY_A, Duration.ofMinutes(15)).generateJwtToken(user());
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));

        assertThat(payload).doesNotContain("anhad@example.com");
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = jwtService(KEY_A, Duration.ofSeconds(-1)); // already expired when issued

        assertThat(jwt.parse(jwt.generateJwtToken(user()))).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String foreignToken = jwtService(KEY_B, Duration.ofMinutes(15)).generateJwtToken(user());

        assertThat(jwtService(KEY_A, Duration.ofMinutes(15)).parse(foreignToken)).isEmpty();
    }

    @Test
    void tamperedPayloadIsRejected() {
        JwtService jwt = jwtService(KEY_A, Duration.ofMinutes(15));
        String[] parts = jwt.generateJwtToken(user()).split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"1\",\"role\":\"ADMIN\"}".getBytes());

        assertThat(jwt.parse(parts[0] + "." + forgedPayload + "." + parts[2])).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(jwtService(KEY_A, Duration.ofMinutes(15)).parse("not.a.jwt")).isEmpty();
    }

    @Test
    void keyShorterThan256BitsFailsAtStartupWithHelpfulMessage() {
        String shortKey = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThatThrownBy(() -> jwtService(shortKey, Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("openssl rand -base64 32");
    }
}
