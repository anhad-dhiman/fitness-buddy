package com.fitnessbuddy.post.service;

import com.fitnessbuddy.common.exception.BadRequestException;
import com.fitnessbuddy.common.exception.UnauthorizedException;
import com.fitnessbuddy.common.mapper.Mapper;
import com.fitnessbuddy.post.dto.CreatePostDto;
import com.fitnessbuddy.post.dto.PostCursor;
import com.fitnessbuddy.post.dto.PostFeedResponse;
import com.fitnessbuddy.post.dto.ResponsePostDto;
import com.fitnessbuddy.post.entity.PostEntity;
import com.fitnessbuddy.post.repository.PostRepo;
import com.fitnessbuddy.user.entity.UserEntity;
import com.fitnessbuddy.user.repository.UserRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class PostService {

    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final Mapper<PostEntity, ResponsePostDto> postMapper;

    public PostService(PostRepo postRepo, Mapper<PostEntity, ResponsePostDto> postMapper,
                       UserRepo userRepo) {
        this.postRepo = postRepo;
        this.postMapper = postMapper;
        this.userRepo = userRepo;
    }

    /**
     * @param authorId the authenticated user's id, which the controller takes
     *                 from the security context, never from the request body
     */
    @Transactional
    public ResponsePostDto newPost(Integer authorId, CreatePostDto createPostDto) {
        // The JWT is valid but its user may have been deleted since it was issued.
        UserEntity author = userRepo.findById(authorId)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));

        PostEntity postEntity = new PostEntity();
        postEntity.setUser(author);
        postEntity.setBody(createPostDto.getBody());
        postEntity.setCreatedAt(Instant.now());

        postRepo.save(postEntity);

        return postMapper.mapToDto(postEntity);
    }

    // readOnly = true: no dirty checking or flush at the end, and it tells
    // readers (and the JDBC driver) that this method never writes.
    @Transactional(readOnly = true)
    public PostFeedResponse getPosts(String cursor) {
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

        return new PostFeedResponse(response, nextCursor, hasMore);
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
            throw new BadRequestException("Invalid cursor");
        }
    }
}
