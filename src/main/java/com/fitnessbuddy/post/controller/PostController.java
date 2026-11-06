package com.fitnessbuddy.post.controller;

import com.fitnessbuddy.post.dto.CreatePostDto;
import com.fitnessbuddy.post.dto.PostFeedResponse;
import com.fitnessbuddy.post.dto.ResponsePostDto;
import com.fitnessbuddy.post.entity.PostEntity;
import com.fitnessbuddy.post.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/post")
public class PostController {

    private PostService postService;

    public PostController(PostService postService){
        this.postService = postService;
    }

    @PostMapping("/newPost")
    public ResponseEntity<ResponsePostDto> newPost(@RequestBody CreatePostDto createPostDto) {
        return postService.newPost(createPostDto);
    }

    @GetMapping("/posts")
    public ResponseEntity<PostFeedResponse> getPosts(@RequestParam(required = false) String cursor) {
        return postService.getPosts(cursor);
    }
}
