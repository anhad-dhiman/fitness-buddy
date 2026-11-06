package com.fitnessbuddy.auth;

import com.fitnessbuddy.TestcontainersConfiguration;
import com.fitnessbuddy.auth.dto.RegisterRequest;
import com.fitnessbuddy.auth.service.AuthService;
import com.fitnessbuddy.common.exception.ConflictException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * End-to-end through every real layer: HTTP → security filters (real CSRF
 * check, real JWT) → controller → service → JPA → PostgreSQL (Testcontainers).
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

    private Browser browser;

    @BeforeEach
    void newBrowser() {
        browser = new Browser();
    }

    @Test
    void fullFlowWithRealCsrfTokens() throws Exception {
        String name = uniqueName();

        assertThat(browser.get("/auth/csrf").getStatus()).isEqualTo(204);
        assertThat(browser.xsrfToken()).isNotNull();
        assertThat(browser.cookie("XSRF-TOKEN").isHttpOnly())
                .as("the SPA's JavaScript must be able to read the CSRF cookie").isFalse();

        assertThat(browser.postJson("/auth/register", registerJson(name, name + "@Example.com")).getStatus())
                .isEqualTo(201);

        String tokenBeforeLogin = browser.xsrfToken();
        assertThat(browser.postJson("/auth/login", loginJson(name + "@example.COM")).getStatus()).isEqualTo(200);
        assertThat(browser.cookie("accessToken")).isNotNull();
        assertThat(browser.xsrfToken()).as("CSRF token rotates at login").isNotEqualTo(tokenBeforeLogin);

        MockHttpServletResponse created = browser.postJson("/post/newPost", "{\"body\":\"" + "x".repeat(1000) + "\"}");
        assertThat(created.getStatus()).isEqualTo(201);
        assertThat(created.getContentAsString()).contains(name);

        String tokenAfterLogin = browser.xsrfToken();
        assertThat(browser.get("/post/posts").getStatus()).isEqualTo(200);
        assertThat(browser.xsrfToken()).as("CSRF token is stable between login and logout").isEqualTo(tokenAfterLogin);

        assertThat(browser.post("/auth/logout").getStatus()).isEqualTo(204);
        assertThat(browser.cookie("accessToken")).as("logout deletes the auth cookie").isNull();
        assertThat(browser.get("/post/posts").getStatus()).isEqualTo(401);

        assertThat(browser.sawSessionCookie).as("the API is stateless: no JSESSIONID ever").isFalse();
    }

    @Test
    void forgedCsrfHeaderIsRejected() throws Exception {
        browser.get("/auth/csrf");

        MockHttpServletResponse response = mockMvc.perform(browser.withCookies(post("/auth/register"))
                        .header("X-XSRF-TOKEN", "forged-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueName(), uniqueName() + "@example.com")))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void tamperedAccessTokenIsTreatedAsUnauthenticated() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/post/posts")
                        .cookie(new Cookie("accessToken", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.forged")))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }

    @Test
    void sameEmailWithDifferentCaseIsRejectedWith409() throws Exception {
        String name = uniqueName();
        browser.get("/auth/csrf");
        assertThat(browser.postJson("/auth/register", registerJson(name, name + "@example.com")).getStatus())
                .isEqualTo(201);

        MockHttpServletResponse duplicate = browser.postJson("/auth/register",
                registerJson(uniqueName(), name.toUpperCase() + "@EXAMPLE.COM"));

        assertThat(duplicate.getStatus()).isEqualTo(409);
        assertThat(duplicate.getContentAsString()).contains("An account with this email already exists");
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

    /**
     * A minimal stand-in for the React app in a browser: it keeps the cookies
     * each response sets (deleting those with Max-Age=0), sends them back, and
     * copies the XSRF-TOKEN cookie into the X-XSRF-TOKEN header on every
     * state-changing request, which is exactly what axios does.
     */
    private class Browser {
        private final Map<String, Cookie> cookies = new HashMap<>();
        boolean sawSessionCookie;

        MockHttpServletResponse get(String path) throws Exception {
            return send(MockMvcRequestBuilders.get(path));
        }

        MockHttpServletResponse post(String path) throws Exception {
            return send(withCsrfHeader(MockMvcRequestBuilders.post(path)));
        }

        MockHttpServletResponse postJson(String path, String json) throws Exception {
            return send(withCsrfHeader(MockMvcRequestBuilders.post(path)).contentType(MediaType.APPLICATION_JSON).content(json));
        }

        String xsrfToken() {
            Cookie c = cookies.get("XSRF-TOKEN");
            return c == null ? null : c.getValue();
        }

        Cookie cookie(String name) {
            return cookies.get(name);
        }

        MockHttpServletRequestBuilder withCookies(MockHttpServletRequestBuilder request) {
            if (!cookies.isEmpty()) {
                request.cookie(cookies.values().toArray(Cookie[]::new));
            }
            return request;
        }

        private MockHttpServletRequestBuilder withCsrfHeader(MockHttpServletRequestBuilder request) {
            String token = xsrfToken();
            return token == null ? request : request.header("X-XSRF-TOKEN", token);
        }

        private MockHttpServletResponse send(MockHttpServletRequestBuilder request) throws Exception {
            MockHttpServletResponse response = mockMvc.perform(withCookies(request)).andReturn().getResponse();
            for (Cookie c : response.getCookies()) {
                if (c.getName().equals("JSESSIONID")) {
                    sawSessionCookie = true;
                }
                if (c.getMaxAge() == 0) {
                    cookies.remove(c.getName());
                } else {
                    cookies.put(c.getName(), c);
                }
            }
            return response;
        }
    }

    private static String uniqueName() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static String registerJson(String userName, String email) {
        return """
                {"userName":"%s","email":"%s","password":"secret123"}""".formatted(userName, email);
    }

    private static String loginJson(String email) {
        return """
                {"email":"%s","password":"secret123"}""".formatted(email);
    }
}
