package com.fitnessBuddy.fitness_buddy.controllers.post;

import com.fitnessBuddy.fitness_buddy.domain.dto.posts.CreatePostDto;
import com.fitnessBuddy.fitness_buddy.domain.dto.posts.PostFeedResponse;
import com.fitnessBuddy.fitness_buddy.domain.dto.posts.ResponsePostDto;
import com.fitnessBuddy.fitness_buddy.domain.entities.PostEntity;
import com.fitnessBuddy.fitness_buddy.services.post.PostService;
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
