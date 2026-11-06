package com.fitnessBuddy.fitness_buddy.services.post;

import com.fitnessBuddy.fitness_buddy.domain.dto.posts.CreatePostDto;
import com.fitnessBuddy.fitness_buddy.domain.dto.posts.PostCursor;
import com.fitnessBuddy.fitness_buddy.domain.dto.posts.PostFeedResponse;
import com.fitnessBuddy.fitness_buddy.domain.dto.posts.ResponsePostDto;
import com.fitnessBuddy.fitness_buddy.domain.entities.PostEntity;
import com.fitnessBuddy.fitness_buddy.domain.entities.UserEntity;
import com.fitnessBuddy.fitness_buddy.mappers.Mapper;
import com.fitnessBuddy.fitness_buddy.repositories.PostRepo;
import com.fitnessBuddy.fitness_buddy.repositories.UserRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {
    PostRepo postRepo;
    UserRepo userRepo;
    private final Mapper<PostEntity, ResponsePostDto> postMapper;

    public PostService(PostRepo postRepo, Mapper<PostEntity, ResponsePostDto> postMapper,
                       UserRepo userRepo) {
        this.postRepo = postRepo;
        this.postMapper = postMapper;
        this.userRepo = userRepo;
    }

    public ResponseEntity<ResponsePostDto> newPost(CreatePostDto createPostDto){
        PostEntity postEntity = new PostEntity();
        String body = createPostDto.getBody();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null){
            Integer userId = Integer.valueOf(authentication.getName());

            Optional<UserEntity> userEntity = userRepo.findById(userId);

            if(userEntity.isPresent()){

                postEntity.setUser(userEntity.get());
                postEntity.setBody(body);
                postEntity.setCreatedAt(Instant.now());

                postRepo.save(postEntity);

                return ResponseEntity.ok(postMapper.mapToDto(postEntity));
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    public ResponseEntity<PostFeedResponse> getPosts(String cursor) {
        int limit = 2;
        List<PostEntity> posts;
        if (cursor == null || cursor.isBlank()) {
            posts = postRepo.findFirstPage(PageRequest.of(0, limit + 1));
        } else {
            PostCursor decodedCursor = decodeCursor(cursor);
            posts = postRepo.findNextPage(
                    decodedCursor.createdAt(),
                    decodedCursor.id(),
                    PageRequest.of(0, limit + 1)
            );
        }
        boolean hasMore = posts.size() > limit;
        if (hasMore) {
            posts = posts.subList(0, limit);
        }

        List<ResponsePostDto> response = posts.stream()
                .map(postMapper::mapToDto)
                .toList();
        String nextCursor = null;

        if (hasMore && !posts.isEmpty()) {
            PostEntity lastPost = posts.getLast();
            nextCursor = encodeCursor(
                    new PostCursor(
                            lastPost.getCreatedAt(),
                            lastPost.getId()
                    )
            );
        }

        return ResponseEntity.ok(new PostFeedResponse(response, nextCursor, hasMore));
    }

    private String encodeCursor(PostCursor c) {
        String raw = c.createdAt().toString() + "_" + c.id();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private PostCursor decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("_", 2);
            return new PostCursor(Instant.parse(parts[0]), Integer.valueOf(parts[1]));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
    }
}
