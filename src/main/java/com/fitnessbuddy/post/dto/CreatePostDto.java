package com.fitnessbuddy.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePostDto {

    // Same limit as the ck_posts_body_length constraint in V2: the API gives
    // a friendly 400, the database guarantees it.
    @NotBlank(message = "Post body must not be empty")
    @Size(max = 5000, message = "Post body must be at most 5000 characters")
    private String body;
}
