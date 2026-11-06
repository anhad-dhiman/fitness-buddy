package com.fitnessbuddy.post.controller;

import com.fitnessbuddy.config.SecurityConfig;
import com.fitnessbuddy.post.dto.ResponsePostDto;
import com.fitnessbuddy.post.service.PostService;
import com.fitnessbuddy.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
@Import(SecurityConfig.class)
class PostControllerTests {

    @Autowired MockMvc mockMvc;

    @MockitoBean PostService postService;
    @MockitoBean JwtService jwtService;

    /** Simulates what JwtAuthenticationFilter does for a valid token of user 42. */
    private static RequestPostProcessor asUser42() {
        return authentication(new UsernamePasswordAuthenticationToken(
                "42", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    void createPostReturns201AndUsesTheAuthenticatedUserId() throws Exception {
        when(postService.newPost(eq(42), any()))
                .thenReturn(new ResponsePostDto(1, "Leg day", null, Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/post/newPost").with(asUser42()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Leg day"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(postService).newPost(eq(42), any());
    }

    @Test
    void blankBodyReturns400() throws Exception {
        mockMvc.perform(post("/post/newPost").with(asUser42()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"   "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body[0]").value("Post body must not be empty"));
        verifyNoInteractions(postService);
    }

    @Test
    void bodyOver5000CharactersReturns400() throws Exception {
        mockMvc.perform(post("/post/newPost").with(asUser42()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + "a".repeat(5001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body[0]").value("Post body must be at most 5000 characters"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        // Currently 403 because no AuthenticationEntryPoint is configured.
        // Milestone 1.4 changes this to the correct 401; update this test then.
        mockMvc.perform(get("/post/posts"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(postService);
    }
}
