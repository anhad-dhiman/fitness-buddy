package com.fitnessbuddy.post.controller;

import com.fitnessbuddy.post.dto.CreatePostDto;
import com.fitnessbuddy.post.dto.PostFeedResponse;
import com.fitnessbuddy.post.dto.ResponsePostDto;
import com.fitnessbuddy.post.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/post")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService){
        this.postService = postService;
    }

    // @AuthenticationPrincipal injects the principal JwtAuthenticationFilter
    // stored in the SecurityContext: the user id from the JWT subject.
    // (It is a String today; Milestone 1.4 makes it a typed principal.)
    @PostMapping("/newPost")
    public ResponseEntity<ResponsePostDto> newPost(@AuthenticationPrincipal String userId,
                                                   @Valid @RequestBody CreatePostDto createPostDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.newPost(Integer.valueOf(userId), createPostDto));
    }

    @GetMapping("/posts")
    public PostFeedResponse getPosts(@RequestParam(required = false) String cursor) {
        return postService.getPosts(cursor);
    }
}
