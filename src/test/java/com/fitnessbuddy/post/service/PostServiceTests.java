package com.fitnessbuddy.post.service;

import com.fitnessbuddy.common.exception.BadRequestException;
import com.fitnessbuddy.common.exception.UnauthorizedException;
import com.fitnessbuddy.post.dto.CreatePostDto;
import com.fitnessbuddy.post.entity.PostEntity;
import com.fitnessbuddy.post.mapper.PostMapperImpl;
import com.fitnessbuddy.post.repository.PostRepo;
import com.fitnessbuddy.user.entity.UserEntity;
import com.fitnessbuddy.user.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTests {

    @Mock PostRepo postRepo;
    @Mock UserRepo userRepo;

    PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepo, new PostMapperImpl(new ModelMapper()), userRepo);
    }

    @Test
    void newPostIsSavedWithTheAuthenticatedUserAsAuthor() {
        UserEntity author = UserEntity.builder().id(7).userName("anhad").role("USER").build();
        when(userRepo.findById(7)).thenReturn(Optional.of(author));

        var response = postService.newPost(7, new CreatePostDto("Leg day done"));

        ArgumentCaptor<PostEntity> saved = ArgumentCaptor.forClass(PostEntity.class);
        verify(postRepo).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(author);
        assertThat(saved.getValue().getBody()).isEqualTo("Leg day done");
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
        assertThat(response.getUser().getUserName()).isEqualTo("anhad");
    }

    @Test
    void newPostForDeletedUserIsUnauthorized() {
        when(userRepo.findById(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.newPost(7, new CreatePostDto("hi")))
                .isInstanceOf(UnauthorizedException.class);
        verify(postRepo, never()).save(any());
    }

    @Test
    void garbageCursorIsBadRequest() {
        assertThatThrownBy(() -> postService.getPosts("not-a-cursor"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid cursor");
    }
}
