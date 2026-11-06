package com.fitnessbuddy.auth;

import com.fitnessbuddy.TestcontainersConfiguration;
import com.fitnessbuddy.auth.dto.RegisterRequest;
import com.fitnessbuddy.auth.service.AuthService;
import com.fitnessbuddy.common.exception.ConflictException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end through every real layer: HTTP → security filter → controller →
 * service → JPA → PostgreSQL (Testcontainers) → and back.
 *
 * There is no per-test rollback here (requests run in their own transactions),
 * so every test uses unique emails/usernames instead of relying on cleanup.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired AuthService authService;

    @Test
    void registerLoginCreatePostAndReadFeed() throws Exception {
        String name = uniqueName();

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(name, name + "@Example.com")))
                .andExpect(status().isCreated());

        Cookie accessToken = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s@example.COM","password":"secret123"}""".formatted(name)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("accessToken");
        assertThat(accessToken).isNotNull();

        mockMvc.perform(post("/post/newPost").cookie(accessToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + "x".repeat(1000) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.userName").value(name));

        mockMvc.perform(get("/post/posts").cookie(accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[0].user.userName").value(name));
    }

    @Test
    void sameEmailWithDifferentCaseIsRejectedWith409() throws Exception {
        String name = uniqueName();
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(name, name + "@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueName(), name.toUpperCase() + "@EXAMPLE.COM")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An account with this email already exists"));
    }

    /**
     * Two registrations for the same email start at the same instant. Both may
     * pass the "exists?" check; the database unique index must make exactly
     * one of them win, and the loser must get a ConflictException (409), not
     * a 500.
     */
    @Test
    void concurrentRegistrationsForTheSameEmailCreateExactlyOneAccount() throws Exception {
        String email = uniqueName() + "@example.com";
        int threads = 8;
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<String>> outcomes = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                outcomes.add(pool.submit(() -> {
                    startTogether.await();
                    try {
                        authService.registerUser(new RegisterRequest(uniqueName(), "secret123", email));
                        return "created";
                    } catch (ConflictException e) {
                        return "conflict";
                    }
                }));
            }
            startTogether.countDown();

            List<String> results = new ArrayList<>();
            for (Future<String> outcome : outcomes) {
                results.add(outcome.get()); // rethrows anything other than ConflictException
            }
            assertThat(results).containsOnlyOnce("created");
            assertThat(results).filteredOn("conflict"::equals).hasSize(threads - 1);
        } finally {
            pool.shutdownNow();
        }
    }

    private static String uniqueName() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static String registerJson(String userName, String email) {
        return """
                {"userName":"%s","email":"%s","password":"secret123"}""".formatted(userName, email);
    }
}
