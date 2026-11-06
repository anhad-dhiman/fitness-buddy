package com.fitnessbuddy.post.controller;

import com.fitnessbuddy.config.CorsConfig;
import com.fitnessbuddy.config.SecurityConfig;
import com.fitnessbuddy.post.dto.ResponsePostDto;
import com.fitnessbuddy.post.service.PostService;
import com.fitnessbuddy.security.AuthenticatedUser;
import com.fitnessbuddy.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class PostControllerTests {

    @Autowired MockMvc mockMvc;

    @MockitoBean PostService postService;
    @MockitoBean JwtService jwtService;

    /** Simulates what JwtAuthenticationFilter does for a valid token of user 42. */
    private static RequestPostProcessor asUser42() {
        return authentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(42, "USER"), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    void createPostReturns201AndUsesTheAuthenticatedUserId() throws Exception {
        when(postService.newPost(eq(42), any()))
                .thenReturn(new ResponsePostDto(1, "Leg day", null, Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/post/newPost").with(asUser42()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Leg day"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(postService).newPost(eq(42), any());
    }

    @Test
    void createPostWithoutCsrfTokenIsRejectedEvenWhenAuthenticated() throws Exception {
        // This is the CSRF attack: the browser attaches the auth cookie
        // automatically, but a foreign site cannot supply the CSRF header.
        mockMvc.perform(post("/post/newPost").with(asUser42()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Leg day"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Missing or invalid CSRF token")));
        verifyNoInteractions(postService);
    }

    @Test
    void blankBodyReturns400() throws Exception {
        mockMvc.perform(post("/post/newPost").with(asUser42()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"   "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body[0]").value("Post body must not be empty"));
        verifyNoInteractions(postService);
    }

    @Test
    void bodyOver5000CharactersReturns400() throws Exception {
        mockMvc.perform(post("/post/newPost").with(asUser42()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + "a".repeat(5001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body[0]").value("Post body must be at most 5000 characters"));
    }

    @Test
    void unauthenticatedRequestReturns401Problem() throws Exception {
        mockMvc.perform(get("/post/posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Authentication is required to access this resource"));
        verifyNoInteractions(postService);
    }

    @Test
    void corsPreflightFromTheFrontendOriginIsAllowedWithCredentials() throws Exception {
        mockMvc.perform(options("/post/newPost")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void corsPreflightFromAnUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/post/newPost")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
